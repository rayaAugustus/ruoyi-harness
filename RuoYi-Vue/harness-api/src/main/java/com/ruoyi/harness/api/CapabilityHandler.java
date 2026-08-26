package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

@FunctionalInterface
public interface CapabilityHandler { JsonNode handle(JsonNode input, CapabilityContext context) throws Exception; }
