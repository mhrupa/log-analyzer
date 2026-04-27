package com.loganalyzer.integration.controller;

import com.loganalyzer.integration.dto.AccessCheckResponse;
import com.loganalyzer.integration.dto.AnalyzeResponse;
import com.loganalyzer.integration.dto.AnalyzeSessionRequest;
import com.loganalyzer.integration.dto.CreateSessionRequest;
import com.loganalyzer.integration.dto.SessionAccessRequest;
import com.loganalyzer.integration.dto.SessionDetailDto;
import com.loganalyzer.integration.dto.SessionSummaryDto;
import com.loganalyzer.integration.model.AnalysisSession;
import com.loganalyzer.integration.model.AnalysisStatus;
import com.loganalyzer.integration.service.AgentAnalysisService;
import com.loganalyzer.integration.service.AnalysisSessionService;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis-sessions")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AnalysisSessionController {
    private final AnalysisSessionService sessionService;
    private final AgentAnalysisService agentAnalysisService;

    public AnalysisSessionController(
            AnalysisSessionService sessionService,
            AgentAnalysisService agentAnalysisService) {
        this.sessionService = sessionService;
        this.agentAnalysisService = agentAnalysisService;
    }

    @PostMapping
    public SessionDetailDto create(@RequestBody CreateSessionRequest request) {
        return sessionService.create(request);
    }

    @GetMapping
    public List<SessionSummaryDto> list() {
        return sessionService.list();
    }

    @GetMapping("/{sessionId}")
    public SessionDetailDto get(@PathVariable String sessionId) {
        return sessionService.get(sessionId);
    }

    @PostMapping("/{sessionId}/access/check")
    public AccessCheckResponse checkAccess(
            @PathVariable String sessionId,
            @RequestBody SessionAccessRequest request) {
        AnalysisSession session = sessionService.getEntity(sessionId);
        return agentAnalysisService.checkAccess(session, request.credentials());
    }

    @PostMapping("/{sessionId}/analyze")
    public AnalyzeResponse analyze(
            @PathVariable String sessionId,
            @RequestBody AnalyzeSessionRequest request) {
        AnalysisSession session = sessionService.getEntity(sessionId);
        return agentAnalysisService.analyze(session, request.credentials());
    }

    @PostMapping("/{sessionId}/pause")
    public SessionDetailDto pause(@PathVariable String sessionId) {
        sessionService.updateStatus(sessionId, AnalysisStatus.PAUSED);
        return sessionService.get(sessionId);
    }

    @PostMapping("/{sessionId}/resume")
    public SessionDetailDto resume(@PathVariable String sessionId) {
        sessionService.updateStatus(sessionId, AnalysisStatus.RUNNING);
        return sessionService.get(sessionId);
    }
}
