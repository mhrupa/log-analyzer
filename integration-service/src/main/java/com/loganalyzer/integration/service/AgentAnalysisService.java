package com.loganalyzer.integration.service;

import com.loganalyzer.integration.config.LogAnalyzerProperties;
import com.loganalyzer.integration.dto.AccessCheckResponse;
import com.loganalyzer.integration.dto.AnalyzeResponse;
import com.loganalyzer.integration.dto.CredentialsDto;
import com.loganalyzer.integration.dto.EvidenceDto;
import com.loganalyzer.integration.dto.LlmResult;
import com.loganalyzer.integration.dto.McpToolResult;
import com.loganalyzer.integration.model.AnalysisCheckpoint;
import com.loganalyzer.integration.model.AnalysisEvidence;
import com.loganalyzer.integration.model.AnalysisSession;
import com.loganalyzer.integration.model.AnalysisStatus;
import com.loganalyzer.integration.model.AnalysisStep;
import com.loganalyzer.integration.repository.AnalysisCheckpointRepository;
import com.loganalyzer.integration.repository.AnalysisEvidenceRepository;
import com.loganalyzer.integration.repository.AnalysisSessionRepository;
import com.loganalyzer.integration.repository.AnalysisStepRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentAnalysisService {
    private final AccessService accessService;
    private final LogAnalyzerProperties properties;
    private final AnalysisSessionRepository sessionRepository;
    private final AnalysisStepRepository stepRepository;
    private final AnalysisEvidenceRepository evidenceRepository;
    private final AnalysisCheckpointRepository checkpointRepository;
    private final CopilotAgentService copilotAgentService;
    private final McpClientService mcpClientService;

    public AgentAnalysisService(
            AccessService accessService,
            LogAnalyzerProperties properties,
            AnalysisSessionRepository sessionRepository,
            AnalysisStepRepository stepRepository,
            AnalysisEvidenceRepository evidenceRepository,
            AnalysisCheckpointRepository checkpointRepository,
            CopilotAgentService copilotAgentService,
            McpClientService mcpClientService) {
        this.accessService = accessService;
        this.properties = properties;
        this.sessionRepository = sessionRepository;
        this.stepRepository = stepRepository;
        this.evidenceRepository = evidenceRepository;
        this.checkpointRepository = checkpointRepository;
        this.copilotAgentService = copilotAgentService;
        this.mcpClientService = mcpClientService;
    }

    @Transactional
    public AccessCheckResponse checkAccess(AnalysisSession session, CredentialsDto credentials) {
        AccessCheckResponse response = accessService.checkAccess(credentials, session.getRegion());
        session.setStatus(AnalysisStatus.ACCESS_CHECKED);
        session.setLatestSummary(response.ready()
                ? "Access validated. Analysis can run."
                : "Access validation found missing credentials or configuration.");
        sessionRepository.save(session);
        stepRepository.save(new AnalysisStep(
                session,
                "ACCESS_CHECK",
                "Validated Jira, CloudWatch, GitHub, and Copilot access.",
                response.checks().toString()));
        checkpointRepository.save(new AnalysisCheckpoint(
                session,
                "{\"next_action\":\"" + (response.ready() ? "run_analysis" : "collect_missing_access") + "\"}"));
        return response;
    }

    @Transactional
    public AnalyzeResponse analyze(AnalysisSession session, CredentialsDto credentials) {
        AccessCheckResponse access = accessService.checkAccess(credentials, session.getRegion());
        if (!access.ready()) {
            session.setStatus(AnalysisStatus.FAILED);
            session.setLatestSummary("Analysis blocked because required access is missing.");
            sessionRepository.save(session);
            checkpointRepository.save(new AnalysisCheckpoint(session, "{\"next_action\":\"collect_missing_access\"}"));
            List<EvidenceDto> evidence = access.checks().stream()
                    .map(check -> new EvidenceDto(null, check.name(), check.status(), check.message(), 0.0, Instant.now()))
                    .toList();
            return new AnalyzeResponse(
                    session.getId(),
                    "Analysis cannot start because required access is missing.",
                    "Missing access or configuration.",
                    "Unknown",
                    "Provide all required credentials and rerun access validation.",
                    0.0,
                    evidence,
                    List.of("Provide missing access.", "Resume the saved analysis session."));
        }

        session.setStatus(AnalysisStatus.RUNNING);
        sessionRepository.save(session);

        List<AnalysisEvidence> evidence = new ArrayList<>();
        evidence.add(recordEvidence(session, "User input", "Problem statement", textOrDefault(session.getProblemStatement(), "No problem statement supplied."), 0.7));
        evidence.add(recordEvidence(session, "Direct logs", "User supplied logs", truncate(textOrDefault(session.getLogs(), "No direct logs supplied."), 1200), 0.6));
        CredentialsDto resolvedCredentials = accessService.mergeWithEnvironment(credentials);

        evidence.add(recordEvidence(session, "Jira", "Ticket context", collectJiraContext(session, resolvedCredentials), 0.55));
        evidence.add(recordEvidence(session, "CloudWatch", "Logs query plan", collectCloudWatchContext(session), 0.4));
        evidence.add(recordEvidence(session, "GitHub", "Repository inspection", collectGitHubContext(session, resolvedCredentials), 0.55));

        recordStep(session, "JIRA_READ", session.getJiraTicketId(), evidence.get(2).getContent());
        recordStep(session, "CLOUDWATCH_QUERY", session.getLogGroups(), evidence.get(3).getContent());
        recordStep(session, "GITHUB_SEARCH", session.getRepositoryUrls(), evidence.get(4).getContent());

        LlmResult llmResult = copilotAgentService.analyze(session, credentials, evidence);
        evidence.add(recordEvidence(session, "Copilot", "LLM RCA", llmResult.response(), "SUCCESS".equals(llmResult.status()) ? 0.75 : 0.35));
        recordStep(session, "LLM_REASONING", llmResult.prompt(), llmResult.response());

        String summary = "Analysis completed with persisted evidence, tool steps, and Copilot LLM interaction logs.";
        session.setStatus(AnalysisStatus.COMPLETED);
        session.setLatestSummary(summary);
        sessionRepository.save(session);
        checkpointRepository.save(new AnalysisCheckpoint(
                session,
                "{\"next_action\":\"review_result\",\"completed_steps\":[\"JIRA_READ\",\"CLOUDWATCH_QUERY\",\"GITHUB_SEARCH\",\"LLM_REASONING\"]}"));

        return new AnalyzeResponse(
                session.getId(),
                summary,
                llmResult.response(),
                "To be determined from live evidence.",
                "Review the Copilot LLM output and implement real Jira, CloudWatch, and GitHub clients behind the recorded tool steps.",
                "SUCCESS".equals(llmResult.status()) ? 0.75 : 0.35,
                evidence.stream().map(AnalysisSessionMapper::toEvidence).toList(),
                List.of(
                        "Point JIRA_MCP_URL to the Jira MCP server and configure JIRA_MCP_ISSUE_TOOL for your server.",
                        "Implement repeated CloudWatch Logs Insights calls with query IDs saved as steps.",
                        "Point GITHUB_MCP_URL to the GitHub MCP server and configure GITHUB_MCP_SEARCH_TOOL for your server.",
                        "Tune the Copilot-compatible prompt and response parser for your gateway schema."));
    }

    private AnalysisEvidence recordEvidence(AnalysisSession session, String source, String title, String content, double confidence) {
        return evidenceRepository.save(new AnalysisEvidence(session, source, title, content, confidence));
    }

    private void recordStep(AnalysisSession session, String stepType, String inputSummary, String outputSummary) {
        stepRepository.save(new AnalysisStep(session, stepType, textOrDefault(inputSummary, "No input supplied."), outputSummary));
    }

    private String collectJiraContext(AnalysisSession session, CredentialsDto credentials) {
        if (!hasText(session.getJiraTicketId())) {
            return "No Jira ticket supplied. User-provided problem statement/logs are the primary context.";
        }
        if (properties.mcp().jira().enabled()) {
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("issueKey", session.getJiraTicketId());
            arguments.put("baseUrl", properties.jira().baseUrl());
            McpToolResult result = mcpClientService.callTool(
                    properties.mcp().jira(),
                    firstText(credentials.jiraToken(), properties.mcp().jira().token()),
                    properties.mcp().jira().issueTool(),
                    arguments);
            return result.success()
                    ? result.content()
                    : "Jira MCP call failed: " + result.error();
        }
        return "Jira ticket " + session.getJiraTicketId() + " would be fetched from " + properties.jira().baseUrl() + ".";
    }

    private String collectCloudWatchContext(AnalysisSession session) {
        String logGroups = hasText(session.getLogGroups()) ? session.getLogGroups().replace("\n", ", ") : "no explicit log groups";
        return "CloudWatch can be queried multiple times in " + session.getEnvironment() + "/" + session.getRegion()
                + " against " + logGroups + ". Each query/result will be persisted as an AnalysisStep.";
    }

    private String collectGitHubContext(AnalysisSession session, CredentialsDto credentials) {
        if (!hasText(session.getRepositoryUrls())) {
            return "No repositories supplied for code cross-check.";
        }
        if (properties.mcp().github().enabled()) {
            List<String> repositoryResults = new ArrayList<>();
            for (String repositoryUrl : session.getRepositoryUrls().split("\\R+")) {
                if (!hasText(repositoryUrl)) {
                    continue;
                }
                Map<String, Object> arguments = new LinkedHashMap<>();
                arguments.put("repository", repositoryUrl.trim());
                arguments.put("query", buildCodeSearchQuery(session));
                arguments.put("environment", session.getEnvironment());
                arguments.put("region", session.getRegion());
                McpToolResult result = mcpClientService.callTool(
                        properties.mcp().github(),
                        firstText(credentials.githubToken(), properties.mcp().github().token()),
                        properties.mcp().github().searchTool(),
                        arguments);
                repositoryResults.add(repositoryUrl.trim() + "\n" + (result.success()
                        ? result.content()
                        : "GitHub MCP call failed: " + result.error()));
            }
            return repositoryResults.isEmpty()
                    ? "No valid repository URLs supplied for GitHub MCP search."
                    : String.join("\n\n", repositoryResults);
        }
        return "GitHub repositories queued for repeated code search/file inspection: " + session.getRepositoryUrls().replace("\n", ", ") + ".";
    }

    private String buildCodeSearchQuery(AnalysisSession session) {
        String source = firstText(session.getProblemStatement(), session.getLogs());
        if (!hasText(source)) {
            return firstText(session.getJiraTicketId(), "error exception failure");
        }
        return truncate(source.replaceAll("\\s+", " "), 240);
    }

    private String textOrDefault(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first : second;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
