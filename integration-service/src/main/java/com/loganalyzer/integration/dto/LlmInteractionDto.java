package com.loganalyzer.integration.dto;

import java.time.Instant;

public record LlmInteractionDto(
        Long id,
        String provider,
        String model,
        String status,
        String prompt,
        String response,
        String errorMessage,
        long latencyMs,
        Instant createdAt) {}
