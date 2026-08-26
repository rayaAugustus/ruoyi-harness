package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

public record RuntimeResponse(String appKey, Long versionId, String traceId, JsonNode page, JsonNode effects) {}
