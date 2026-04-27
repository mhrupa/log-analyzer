package com.loganalyzer.integration.dto;

import com.loganalyzer.integration.model.AnalysisStatus;
import java.time.Instant;
import java.util.List;

public record SessionDetailDto(
        String id,
        AnalysisStatus status,
        String environment,
        String region,
        AnalysisInputDto input,
        String latestSummary,
        String latestCheckpoint,
        List<StepDto> steps,
        List<EvidenceDto> evidence,
        List<LlmInteractionDto> llmInteractions,
        Instant createdAt,
        Instant updatedAt) {}
