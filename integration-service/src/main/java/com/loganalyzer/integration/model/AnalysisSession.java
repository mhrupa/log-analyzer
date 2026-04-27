package com.loganalyzer.integration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

@Entity
public class AnalysisSession {
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private String region;

    private String jiraTicketId;

    @Lob
    private String problemStatement;

    @Lob
    private String logs;

    @Lob
    private String repositoryUrls;

    @Lob
    private String logGroups;

    @Lob
    private String latestSummary;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected AnalysisSession() {}

    public AnalysisSession(String environment, String region) {
        this.id = UUID.randomUUID().toString();
        this.status = AnalysisStatus.CREATED;
        this.environment = environment;
        this.region = region;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getJiraTicketId() {
        return jiraTicketId;
    }

    public void setJiraTicketId(String jiraTicketId) {
        this.jiraTicketId = jiraTicketId;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public String getRepositoryUrls() {
        return repositoryUrls;
    }

    public void setRepositoryUrls(String repositoryUrls) {
        this.repositoryUrls = repositoryUrls;
    }

    public String getLogGroups() {
        return logGroups;
    }

    public void setLogGroups(String logGroups) {
        this.logGroups = logGroups;
    }

    public String getLatestSummary() {
        return latestSummary;
    }

    public void setLatestSummary(String latestSummary) {
        this.latestSummary = latestSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
