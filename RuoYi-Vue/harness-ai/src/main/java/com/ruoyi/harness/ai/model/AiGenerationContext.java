package com.ruoyi.harness.ai.model;

import com.ruoyi.harness.api.Diagnostic;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record AiGenerationContext(String sdkVersion, String sdkContract, JsonNode uiContract,
        List<CapabilityProjection> capabilities, AppContext app, String currentSource,
        List<Diagnostic> diagnostics, List<String> securityConstraints) {
    public AiGenerationContext {
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        securityConstraints = securityConstraints == null ? List.of() : List.copyOf(securityConstraints);
    }
    public record AppContext(String appKey, String name, Long versionId, Long versionNo, String versionStatus) {}
}
