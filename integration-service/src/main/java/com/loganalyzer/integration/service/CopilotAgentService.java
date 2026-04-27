package com.loganalyzer.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.integration.config.LogAnalyzerProperties;
import com.loganalyzer.integration.dto.CredentialsDto;
import com.loganalyzer.integration.dto.LlmResult;
import com.loganalyzer.integration.model.AnalysisEvidence;
import com.loganalyzer.integration.model.AnalysisSession;
import com.loganalyzer.integration.model.LlmInteraction;
import com.loganalyzer.integration.repository.LlmInteractionRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CopilotAgentService {
    private final LogAnalyzerProperties properties;
    private final AccessService accessService;
    private final LlmInteractionRepository llmInteractionRepository;
    private final ObjectMapper objectMapper;

    public CopilotAgentService(
            LogAnalyzerProperties properties,
            AccessService accessService,
            LlmInteractionRepository llmInteractionRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.accessService = accessService;
        this.llmInteractionRepository = llmInteractionRepository;
        this.objectMapper = objectMapper;
    }

    public LlmResult analyze(AnalysisSession session, CredentialsDto requestCredentials, List<AnalysisEvidence> evidence) {
        String prompt = buildPrompt(session, evidence);
        CredentialsDto credentials = accessService.mergeWithEnvironment(requestCredentials);
        long startedAt = System.nanoTime();

        if (!hasText(properties.copilot().apiUrl())) {
            String response = fallbackResponse("COPILOT_API_URL is not configured. LLM call skipped.", evidence);
            LlmResult result = new LlmResult("SKIPPED", prompt, response, "Missing COPILOT_API_URL", elapsedMs(startedAt));
            persist(session, result);
            return result;
        }

        if (!hasText(credentials.copilotToken())) {
            String response = fallbackResponse("COPILOT_TOKEN is not configured. LLM call skipped.", evidence);
            LlmResult result = new LlmResult("SKIPPED", prompt, response, "Missing COPILOT_TOKEN", elapsedMs(startedAt));
            persist(session, result);
            return result;
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", properties.copilot().model(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt()),
                            Map.of("role", "user", "content", prompt)),
                    "temperature", 0.1));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.copilot().apiUrl()))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.copilot().timeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + credentials.copilotToken())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            String content = extractContent(response.body());
            String status = response.statusCode() >= 200 && response.statusCode() < 300 ? "SUCCESS" : "FAILED";
            String error = "SUCCESS".equals(status) ? null : "LLM endpoint returned HTTP " + response.statusCode();
            LlmResult result = new LlmResult(status, prompt, content, error, elapsedMs(startedAt));
            persist(session, result);
            return result;
        } catch (Exception ex) {
            String error = ex.getClass().getSimpleName() + (hasText(ex.getMessage()) ? ": " + ex.getMessage() : "");
            String response = fallbackResponse("LLM call failed: " + error, evidence);
            LlmResult result = new LlmResult("FAILED", prompt, response, error, elapsedMs(startedAt));
            persist(session, result);
            return result;
        }
    }

    private void persist(AnalysisSession session, LlmResult result) {
        llmInteractionRepository.save(new LlmInteraction(
                session,
                properties.copilot().provider(),
                properties.copilot().model(),
                result.status(),
                result.prompt(),
                result.response(),
                result.errorMessage(),
                result.latencyMs()));
    }

    private String buildPrompt(AnalysisSession session, List<AnalysisEvidence> evidence) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this production bug and produce a concise RCA.\n\n");
        prompt.append("Environment: ").append(session.getEnvironment()).append('\n');
        prompt.append("AWS region: ").append(session.getRegion()).append('\n');
        prompt.append("Jira ticket: ").append(textOrDefault(session.getJiraTicketId(), "not supplied")).append("\n\n");
        prompt.append("Evidence:\n");
        for (AnalysisEvidence item : evidence) {
            prompt.append("- Source: ").append(item.getSource()).append('\n');
            prompt.append("  Title: ").append(item.getTitle()).append('\n');
            prompt.append("  Content: ").append(truncate(textOrDefault(item.getContent(), ""), 2500)).append("\n\n");
        }
        prompt.append("Return these fields: summary, probable_root_cause, impacted_component, suggested_solution, confidence, next_actions.");
        return prompt.toString();
    }

    private String systemPrompt() {
        return "You are a senior incident analysis agent. Use only supplied evidence. "
                + "If evidence is insufficient, say exactly what data is missing. "
                + "Prefer actionable root-cause hypotheses with code/log references.";
    }

    private String extractContent(String body) {
        if (!hasText(body)) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode content = choices.get(0).path("message").path("content");
                if (content.isTextual()) {
                    return content.asText();
                }
            }
            JsonNode output = root.path("output");
            if (output.isTextual()) {
                return output.asText();
            }
            JsonNode content = root.path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            return body;
        } catch (Exception ignored) {
            return body;
        }
    }

    private String fallbackResponse(String reason, List<AnalysisEvidence> evidence) {
        return reason + "\nEvidence items collected: " + evidence.size()
                + ". Continue by configuring the Copilot-compatible endpoint or reviewing persisted evidence manually.";
    }

    private long elapsedMs(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String textOrDefault(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
