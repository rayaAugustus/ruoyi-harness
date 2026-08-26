package com.ruoyi.harness.api;

import tools.jackson.databind.JsonNode;

public record ActionRequest(Long versionId, JsonNode input, JsonNode clientState) {}
