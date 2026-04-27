package com.loganalyzer.integration.controller;

import com.loganalyzer.integration.config.LogAnalyzerProperties;
import com.loganalyzer.integration.dto.ConfigResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {
    private final LogAnalyzerProperties properties;

    public SystemController(LogAnalyzerProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        return new ConfigResponse(
                hasText(properties.jira().baseUrl()),
                hasText(properties.jira().token()),
                hasText(properties.github().token()),
                hasText(properties.copilot().token()),
                hasText(properties.copilot().apiUrl()),
                properties.copilot().provider(),
                properties.copilot().model(),
                hasText(properties.aws().accessKeyId())
                        && hasText(properties.aws().secretAccessKey())
                        && hasText(properties.aws().sessionToken()),
                properties.aws().defaultRegion(),
                "Tokens are read from request credentials or environment variables and are not persisted.");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
