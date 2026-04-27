package com.loganalyzer.integration.dto;

public record ConfigResponse(
        boolean jiraBaseUrlConfigured,
        boolean jiraTokenConfigured,
        boolean githubTokenConfigured,
        boolean jiraMcpConfigured,
        boolean githubMcpConfigured,
        boolean copilotTokenConfigured,
        boolean copilotEndpointConfigured,
        String copilotProvider,
        String copilotModel,
        boolean awsCredentialsConfigured,
        String defaultAwsRegion,
        String credentialStorage) {}
