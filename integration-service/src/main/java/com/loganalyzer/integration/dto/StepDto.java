package com.loganalyzer.integration.dto;

import java.time.Instant;

public record StepDto(
        Long id,
        String stepType,
        String inputSummary,
        String outputSummary,
        Instant createdAt) {}
