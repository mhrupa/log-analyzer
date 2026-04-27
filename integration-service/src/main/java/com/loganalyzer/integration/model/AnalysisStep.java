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
public class AnalysisStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private AnalysisSession session;

    @Column(nullable = false)
    private String stepType;

    @Lob
    private String inputSummary;

    @Lob
    private String outputSummary;

    @Column(nullable = false)
    private Instant createdAt;

    protected AnalysisStep() {}

    public AnalysisStep(AnalysisSession session, String stepType, String inputSummary, String outputSummary) {
        this.session = session;
        this.stepType = stepType;
        this.inputSummary = inputSummary;
        this.outputSummary = outputSummary;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getStepType() {
        return stepType;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
