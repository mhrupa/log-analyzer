package com.loganalyzer.integration.dto;

public record CredentialsDto(
        String jiraToken,
        String githubToken,
        String copilotToken,
        String awsAccessKeyId,
        String awsSecretAccessKey,
        String awsSessionToken) {}
