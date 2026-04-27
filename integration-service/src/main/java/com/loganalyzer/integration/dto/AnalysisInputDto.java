package com.loganalyzer.integration.dto;

import java.util.List;

public record AnalysisInputDto(
        String jiraTicketId,
        String problemStatement,
        String logs,
        List<String> repositoryUrls,
        List<String> logGroups) {}
