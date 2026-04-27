package com.loganalyzer.integration.dto;

public record ConfigResponse(
        boolean jiraBaseUrlConfigured,
        boolean jiraTokenConfigured,
        boolean githubTokenConfigured,
        boolean copilotTokenConfigured,
        boolean copilotEndpointConfigured,
        String copilotProvider,
        String copilotModel,
        boolean awsCredentialsConfigured,
        String defaultAwsRegion,
        String credentialStorage) {}
