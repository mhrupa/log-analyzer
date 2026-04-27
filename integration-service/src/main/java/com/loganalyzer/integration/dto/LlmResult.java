package com.loganalyzer.integration.dto;

public record LlmResult(
        String status,
        String prompt,
        String response,
        String errorMessage,
        long latencyMs) {}
