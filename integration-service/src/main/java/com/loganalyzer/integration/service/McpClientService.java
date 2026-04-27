package com.loganalyzer.integration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.integration.config.LogAnalyzerProperties;
import com.loganalyzer.integration.dto.McpToolResult;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class McpClientService {
    private final ObjectMapper objectMapper;

    public McpClientService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpToolResult callTool(
            LogAnalyzerProperties.McpServer server,
            String bearerToken,
            String toolName,
            Map<String, Object> arguments) {
        if (server == null || !server.enabled()) {
            return new McpToolResult(false, "", "MCP server is not enabled.");
        }
        if (!hasText(server.url())) {
            return new McpToolResult(false, "", "MCP server URL is not configured.");
        }
        if (!hasText(toolName)) {
            return new McpToolResult(false, "", "MCP tool name is not configured.");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", System.currentTimeMillis());
        request.put("method", "tools/call");
        request.put("params", Map.of(
                "name", toolName,
                "arguments", arguments == null ? Map.of() : arguments));

        try {
            RestClient.RequestBodySpec requestSpec = RestClient.builder()
                    .requestFactory(ClientHttpRequestFactories.withTimeout(Duration.ofSeconds(timeout(server))))
                    .build()
                    .post()
                    .uri(server.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);

            if (hasText(bearerToken)) {
                requestSpec.header("Authorization", "Bearer " + bearerToken);
            }

            String response = requestSpec.body(request).retrieve().body(String.class);
            return parseResponse(response);
        } catch (Exception ex) {
            return new McpToolResult(false, "", ex.getMessage());
        }
    }

    private McpToolResult parseResponse(String response) throws JsonProcessingException {
        if (!hasText(response)) {
            return new McpToolResult(false, "", "Empty MCP response.");
        }

        JsonNode root = objectMapper.readTree(response);
        if (root.hasNonNull("error")) {
            return new McpToolResult(false, "", root.path("error").toString());
        }

        JsonNode result = root.path("result");
        JsonNode content = result.path("content");
        if (content.isArray()) {
            StringBuilder builder = new StringBuilder();
            content.forEach(item -> {
                JsonNode text = item.path("text");
                builder.append(text.isMissingNode() ? item.toString() : text.asText()).append("\n");
            });
            return new McpToolResult(true, builder.toString().trim(), "");
        }

        return new McpToolResult(true, result.isMissingNode() ? root.toString() : result.toString(), "");
    }

    private int timeout(LogAnalyzerProperties.McpServer server) {
        return server.timeoutSeconds() > 0 ? server.timeoutSeconds() : 30;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
