package com.loganalyzer.integration.repository;

import com.loganalyzer.integration.model.LlmInteraction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmInteractionRepository extends JpaRepository<LlmInteraction, Long> {
    List<LlmInteraction> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
