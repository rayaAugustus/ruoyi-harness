package com.ruoyi.harness.ai.model;

import com.ruoyi.harness.api.RiskLevel;
import tools.jackson.databind.JsonNode;

public record CapabilityProjection(String name, String version, String description,
        JsonNode inputSchema, JsonNode outputSchema, String requiredPermission, RiskLevel riskLevel) {}
