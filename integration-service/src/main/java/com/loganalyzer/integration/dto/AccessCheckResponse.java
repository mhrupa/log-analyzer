package com.loganalyzer.integration.dto;

import java.util.List;

public record AccessCheckResponse(boolean ready, List<AccessCheckDto> checks) {}
