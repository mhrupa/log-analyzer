package com.loganalyzer.integration.service;

import com.loganalyzer.integration.dto.AnalysisInputDto;
import com.loganalyzer.integration.dto.EvidenceDto;
import com.loganalyzer.integration.dto.LlmInteractionDto;
import com.loganalyzer.integration.dto.SessionDetailDto;
import com.loganalyzer.integration.dto.SessionSummaryDto;
import com.loganalyzer.integration.dto.StepDto;
import com.loganalyzer.integration.model.AnalysisCheckpoint;
import com.loganalyzer.integration.model.AnalysisEvidence;
import com.loganalyzer.integration.model.AnalysisSession;
import com.loganalyzer.integration.model.AnalysisStep;
import com.loganalyzer.integration.model.LlmInteraction;
import java.util.Arrays;
import java.util.List;

public final class AnalysisSessionMapper {
    private AnalysisSessionMapper() {}

    public static SessionSummaryDto toSummary(AnalysisSession session) {
        return new SessionSummaryDto(
                session.getId(),
                session.getStatus(),
                session.getEnvironment(),
                session.getRegion(),
                session.getJiraTicketId(),
                session.getLatestSummary(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    public static SessionDetailDto toDetail(
            AnalysisSession session,
            List<AnalysisStep> steps,
            List<AnalysisEvidence> evidence,
            List<LlmInteraction> llmInteractions,
            AnalysisCheckpoint checkpoint) {
        return new SessionDetailDto(
                session.getId(),
                session.getStatus(),
                session.getEnvironment(),
                session.getRegion(),
                new AnalysisInputDto(
                        session.getJiraTicketId(),
                        session.getProblemStatement(),
                        session.getLogs(),
                        splitLines(session.getRepositoryUrls()),
                        splitLines(session.getLogGroups())),
                session.getLatestSummary(),
                checkpoint == null ? null : checkpoint.getCheckpointJson(),
                steps.stream().map(AnalysisSessionMapper::toStep).toList(),
                evidence.stream().map(AnalysisSessionMapper::toEvidence).toList(),
                llmInteractions.stream().map(AnalysisSessionMapper::toLlmInteraction).toList(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    public static StepDto toStep(AnalysisStep step) {
        return new StepDto(step.getId(), step.getStepType(), step.getInputSummary(), step.getOutputSummary(), step.getCreatedAt());
    }

    public static EvidenceDto toEvidence(AnalysisEvidence evidence) {
        return new EvidenceDto(
                evidence.getId(),
                evidence.getSource(),
                evidence.getTitle(),
                evidence.getContent(),
                evidence.getConfidence(),
                evidence.getCreatedAt());
    }

    public static LlmInteractionDto toLlmInteraction(LlmInteraction interaction) {
        return new LlmInteractionDto(
                interaction.getId(),
                interaction.getProvider(),
                interaction.getModel(),
                interaction.getStatus(),
                interaction.getPrompt(),
                interaction.getResponse(),
                interaction.getErrorMessage(),
                interaction.getLatencyMs(),
                interaction.getCreatedAt());
    }

    public static String joinLines(List<String> values) {
        return values == null ? "" : String.join("\n", values);
    }

    public static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
