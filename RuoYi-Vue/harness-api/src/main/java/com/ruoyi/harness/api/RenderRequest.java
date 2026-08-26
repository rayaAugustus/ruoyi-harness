package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

public record RenderRequest(JsonNode route, JsonNode state) {}
