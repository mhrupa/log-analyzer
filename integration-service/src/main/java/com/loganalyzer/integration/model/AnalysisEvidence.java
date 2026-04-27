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
public class AnalysisEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private AnalysisSession session;

    @Column(nullable = false)
    private String source;

    private String title;

    @Lob
    private String content;

    private double confidence;

    @Column(nullable = false)
    private Instant createdAt;

    protected AnalysisEvidence() {}

    public AnalysisEvidence(AnalysisSession session, String source, String title, String content, double confidence) {
        this.session = session;
        this.source = source;
        this.title = title;
        this.content = content;
        this.confidence = confidence;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public double getConfidence() {
        return confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
