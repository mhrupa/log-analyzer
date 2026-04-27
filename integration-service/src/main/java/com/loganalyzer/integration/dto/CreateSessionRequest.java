package com.loganalyzer.integration.dto;

public record CreateSessionRequest(
        String environment,
        String region,
        AnalysisInputDto input) {}
