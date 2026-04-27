package com.loganalyzer.integration.repository;

import com.loganalyzer.integration.model.AnalysisStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisStepRepository extends JpaRepository<AnalysisStep, Long> {
    List<AnalysisStep> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
