package com.loganalyzer.integration.dto;

import java.time.Instant;

public record EvidenceDto(
        Long id,
        String source,
        String title,
        String content,
        double confidence,
        Instant createdAt) {}
