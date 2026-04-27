package com.loganalyzer.integration.repository;

import com.loganalyzer.integration.model.AnalysisEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisEvidenceRepository extends JpaRepository<AnalysisEvidence, Long> {
    List<AnalysisEvidence> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
