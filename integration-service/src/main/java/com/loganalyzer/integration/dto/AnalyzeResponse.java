package com.loganalyzer.integration.dto;

import java.util.List;

public record AnalyzeResponse(
        String sessionId,
        String summary,
        String probableRootCause,
        String impactedComponent,
        String suggestedSolution,
        double confidence,
        List<EvidenceDto> evidence,
        List<String> nextActions) {}
