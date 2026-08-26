package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface CapabilityInvoker { JsonNode invoke(String name, JsonNode input, CapabilityContext context); }
