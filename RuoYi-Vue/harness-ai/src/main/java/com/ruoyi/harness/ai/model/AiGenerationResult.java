package com.ruoyi.harness.ai.model;

import java.util.List;

public record AiGenerationResult(String assistantMessage, String script,
        List<String> capabilitiesUsed, String model, String provider, Usage usage) {
    public AiGenerationResult {
        capabilitiesUsed = capabilitiesUsed == null ? List.of() : List.copyOf(capabilitiesUsed);
        usage = usage == null ? Usage.empty() : usage;
    }
}
