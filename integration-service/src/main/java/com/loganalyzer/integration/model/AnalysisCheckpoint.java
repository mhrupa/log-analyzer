package com.loganalyzer.integration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import java.time.Instant;

@Entity
public class AnalysisCheckpoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private AnalysisSession session;

    @Lob
    @Column(nullable = false)
    private String checkpointJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected AnalysisCheckpoint() {}

    public AnalysisCheckpoint(AnalysisSession session, String checkpointJson) {
        this.session = session;
        this.checkpointJson = checkpointJson;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCheckpointJson() {
        return checkpointJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
