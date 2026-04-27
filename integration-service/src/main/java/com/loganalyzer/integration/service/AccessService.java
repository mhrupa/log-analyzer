package com.loganalyzer.integration.service;

import com.loganalyzer.integration.config.LogAnalyzerProperties;
import com.loganalyzer.integration.dto.AccessCheckDto;
import com.loganalyzer.integration.dto.AccessCheckResponse;
import com.loganalyzer.integration.dto.CredentialsDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccessService {
    private final LogAnalyzerProperties properties;

    public AccessService(LogAnalyzerProperties properties) {
        this.properties = properties;
    }

    public AccessCheckResponse checkAccess(CredentialsDto requestCredentials, String region) {
        CredentialsDto credentials = mergeWithEnvironment(requestCredentials);
        List<AccessCheckDto> checks = List.of(
                checkJira(credentials.jiraToken()),
                checkCloudWatch(credentials, region),
                checkGitHub(credentials.githubToken()),
                checkCopilot(credentials.copilotToken()));
        boolean ready = checks.stream().allMatch(check -> "configured".equals(check.status()) || "ready".equals(check.status()));
        return new AccessCheckResponse(ready, checks);
    }

    public CredentialsDto mergeWithEnvironment(CredentialsDto credentials) {
        CredentialsDto safeCredentials = credentials == null ? new CredentialsDto(null, null, null, null, null, null) : credentials;
        return new CredentialsDto(
                firstText(safeCredentials.jiraToken(), properties.jira().token()),
                firstText(safeCredentials.githubToken(), properties.github().token()),
                firstText(safeCredentials.copilotToken(), properties.copilot().token()),
                firstText(safeCredentials.awsAccessKeyId(), properties.aws().accessKeyId()),
                firstText(safeCredentials.awsSecretAccessKey(), properties.aws().secretAccessKey()),
                firstText(safeCredentials.awsSessionToken(), properties.aws().sessionToken()));
    }

    private AccessCheckDto checkJira(String token) {
        if (!hasText(properties.jira().baseUrl())) {
            return new AccessCheckDto("Jira", "missing", "Set JIRA_BASE_URL before using Jira lookup.");
        }
        if (!hasText(token)) {
            return new AccessCheckDto("Jira", "missing", "Provide a Jira API token or set JIRA_TOKEN for this process.");
        }
        return new AccessCheckDto("Jira", "configured", "Jira base URL configured: " + properties.jira().baseUrl());
    }

    private AccessCheckDto checkCloudWatch(CredentialsDto credentials, String region) {
        List<String> missing = new ArrayList<>();
        if (!hasText(credentials.awsAccessKeyId())) {
            missing.add("AWS access key id");
        }
        if (!hasText(credentials.awsSecretAccessKey())) {
            missing.add("AWS secret access key");
        }
        if (!hasText(credentials.awsSessionToken())) {
            missing.add("AWS session token");
        }
        if (!missing.isEmpty()) {
            return new AccessCheckDto("AWS CloudWatch", "missing", "Provide " + String.join(", ", missing) + " for " + region + ".");
        }
        return new AccessCheckDto("AWS CloudWatch", "configured", "Session credentials supplied for " + region + ".");
    }

    private AccessCheckDto checkGitHub(String token) {
        if (!hasText(token)) {
            return new AccessCheckDto("GitHub", "missing", "Provide a GitHub token or set GITHUB_TOKEN with repository read access.");
        }
        return new AccessCheckDto("GitHub", "configured", "GitHub token supplied for repository inspection.");
    }

    private AccessCheckDto checkCopilot(String token) {
        if (!hasText(token)) {
            return new AccessCheckDto("Copilot", "missing", "Provide a Copilot-compatible token or set COPILOT_TOKEN.");
        }
        if (!hasText(properties.copilot().apiUrl())) {
            return new AccessCheckDto("Copilot", "missing", "Set COPILOT_API_URL for your organization's Copilot-compatible LLM gateway.");
        }
        return new AccessCheckDto("Copilot", "configured", "Copilot-compatible LLM endpoint configured for agent reasoning.");
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
