package com.loganalyzer.integration.dto;

public record McpToolResult(
        boolean success,
        String content,
        String error) {}
