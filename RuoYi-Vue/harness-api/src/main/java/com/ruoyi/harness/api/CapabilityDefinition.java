package com.ruoyi.harness.api;

import java.util.Objects;
import tools.jackson.databind.JsonNode;

public record CapabilityDefinition(String name, String version, String description,
        JsonNode inputSchema, JsonNode outputSchema, String requiredPermission,
        RiskLevel riskLevel, CapabilityHandler handler) {
    public CapabilityDefinition {
        Objects.requireNonNull(name); Objects.requireNonNull(version); Objects.requireNonNull(riskLevel);
        Objects.requireNonNull(handler);
    }
}
