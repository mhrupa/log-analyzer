package com.loganalyzer.integration.dto;

import com.loganalyzer.integration.model.AnalysisStatus;
import java.time.Instant;

public record SessionSummaryDto(
        String id,
        AnalysisStatus status,
        String environment,
        String region,
        String jiraTicketId,
        String latestSummary,
        Instant createdAt,
        Instant updatedAt) {}
