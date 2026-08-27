package com.ruoyi.harness.ai.model;

import java.util.List;

public record AiGenerationRequest(List<AiMessage> messages, AiGenerationContext context,
        AiGenerationOptions options) {
    public AiGenerationRequest { messages = messages == null ? List.of() : List.copyOf(messages); }
}
