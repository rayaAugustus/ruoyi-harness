package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

public record ScriptExecutionContext(String entryType, String entryName, JsonNode input,
        JsonNode clientState, RuntimeIdentity identity, String locale, String requestId,
        String traceId, CapabilityInvoker capabilityInvoker) {}
