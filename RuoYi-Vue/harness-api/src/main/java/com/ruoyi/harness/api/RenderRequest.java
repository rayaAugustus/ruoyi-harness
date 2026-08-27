package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

public record RenderRequest(JsonNode route, JsonNode state, Long versionId) {
    public RenderRequest(JsonNode route, JsonNode state) { this(route, state, null); }
}
