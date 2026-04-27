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
public class LlmInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private AnalysisSession session;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String status;

    @Lob
    private String prompt;

    @Lob
    private String response;

    @Lob
    private String errorMessage;

    private long latencyMs;

    @Column(nullable = false)
    private Instant createdAt;

    protected LlmInteraction() {}

    public LlmInteraction(
            AnalysisSession session,
            String provider,
            String model,
            String status,
            String prompt,
            String response,
            String errorMessage,
            long latencyMs) {
        this.session = session;
        this.provider = provider;
        this.model = model;
        this.status = status;
        this.prompt = prompt;
        this.response = response;
        this.errorMessage = errorMessage;
        this.latencyMs = latencyMs;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getStatus() {
        return status;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
