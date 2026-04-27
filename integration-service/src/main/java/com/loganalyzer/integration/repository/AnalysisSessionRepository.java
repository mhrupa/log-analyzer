package com.loganalyzer.integration.repository;

import com.loganalyzer.integration.model.AnalysisSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisSessionRepository extends JpaRepository<AnalysisSession, String> {
    List<AnalysisSession> findTop20ByOrderByUpdatedAtDesc();
}
