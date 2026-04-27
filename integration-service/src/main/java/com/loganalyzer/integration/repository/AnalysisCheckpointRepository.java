package com.loganalyzer.integration.repository;

import com.loganalyzer.integration.model.AnalysisCheckpoint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisCheckpointRepository extends JpaRepository<AnalysisCheckpoint, Long> {
    Optional<AnalysisCheckpoint> findTopBySessionIdOrderByCreatedAtDesc(String sessionId);
}
