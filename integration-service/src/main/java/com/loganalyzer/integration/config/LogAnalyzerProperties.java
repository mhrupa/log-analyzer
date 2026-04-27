package com.loganalyzer.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loganalyzer")
public record LogAnalyzerProperties(
        Jira jira,
        Github github,
        Copilot copilot,
        Aws aws) {

    public record Jira(String baseUrl, String token) {}

    public record Github(String token) {}

    public record Copilot(
            String token,
            String apiUrl,
            String model,
            String provider,
            int timeoutSeconds) {}

    public record Aws(
            String accessKeyId,
            String secretAccessKey,
            String sessionToken,
            String defaultRegion) {}
}
