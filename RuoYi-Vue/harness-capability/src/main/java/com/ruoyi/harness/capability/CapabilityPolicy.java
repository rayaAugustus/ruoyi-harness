package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.CapabilityContext;
import com.ruoyi.harness.api.CapabilityDefinition;
import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface CapabilityPolicy {
    boolean isAllowed(CapabilityDefinition definition, JsonNode input, CapabilityContext context);
    static CapabilityPolicy allowAll() { return (definition, input, context) -> true; }
}
