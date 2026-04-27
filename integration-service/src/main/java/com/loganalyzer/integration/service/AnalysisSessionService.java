package com.loganalyzer.integration.service;

import com.loganalyzer.integration.dto.AnalysisInputDto;
import com.loganalyzer.integration.dto.CreateSessionRequest;
import com.loganalyzer.integration.dto.SessionDetailDto;
import com.loganalyzer.integration.dto.SessionSummaryDto;
import com.loganalyzer.integration.model.AnalysisCheckpoint;
import com.loganalyzer.integration.model.AnalysisSession;
import com.loganalyzer.integration.model.AnalysisStatus;
import com.loganalyzer.integration.repository.AnalysisCheckpointRepository;
import com.loganalyzer.integration.repository.AnalysisEvidenceRepository;
import com.loganalyzer.integration.repository.AnalysisSessionRepository;
import com.loganalyzer.integration.repository.AnalysisStepRepository;
import com.loganalyzer.integration.repository.LlmInteractionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisSessionService {
    private final AnalysisSessionRepository sessionRepository;
    private final AnalysisStepRepository stepRepository;
    private final AnalysisEvidenceRepository evidenceRepository;
    private final AnalysisCheckpointRepository checkpointRepository;
    private final LlmInteractionRepository llmInteractionRepository;

    public AnalysisSessionService(
            AnalysisSessionRepository sessionRepository,
            AnalysisStepRepository stepRepository,
            AnalysisEvidenceRepository evidenceRepository,
            AnalysisCheckpointRepository checkpointRepository,
            LlmInteractionRepository llmInteractionRepository) {
        this.sessionRepository = sessionRepository;
        this.stepRepository = stepRepository;
        this.evidenceRepository = evidenceRepository;
        this.checkpointRepository = checkpointRepository;
        this.llmInteractionRepository = llmInteractionRepository;
    }

    @Transactional
    public SessionDetailDto create(CreateSessionRequest request) {
        AnalysisSession session = new AnalysisSession(request.environment(), request.region());
        applyInput(session, request.input());
        sessionRepository.save(session);
        checkpointRepository.save(new AnalysisCheckpoint(session, "{\"next_action\":\"validate_access\"}"));
        return get(session.getId());
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryDto> list() {
        return sessionRepository.findTop20ByOrderByUpdatedAtDesc().stream()
                .map(AnalysisSessionMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionDetailDto get(String sessionId) {
        AnalysisSession session = getEntity(sessionId);
        AnalysisCheckpoint checkpoint = checkpointRepository.findTopBySessionIdOrderByCreatedAtDesc(sessionId).orElse(null);
        return AnalysisSessionMapper.toDetail(
                session,
                stepRepository.findBySessionIdOrderByCreatedAtAsc(sessionId),
                evidenceRepository.findBySessionIdOrderByCreatedAtAsc(sessionId),
                llmInteractionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId),
                checkpoint);
    }

    @Transactional
    public void updateStatus(String sessionId, AnalysisStatus status) {
        AnalysisSession session = getEntity(sessionId);
        session.setStatus(status);
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public AnalysisSession getEntity(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Analysis session not found: " + sessionId));
    }

    private void applyInput(AnalysisSession session, AnalysisInputDto input) {
        if (input == null) {
            return;
        }
        session.setJiraTicketId(input.jiraTicketId());
        session.setProblemStatement(input.problemStatement());
        session.setLogs(input.logs());
        session.setRepositoryUrls(AnalysisSessionMapper.joinLines(input.repositoryUrls()));
        session.setLogGroups(AnalysisSessionMapper.joinLines(input.logGroups()));
    }
}
