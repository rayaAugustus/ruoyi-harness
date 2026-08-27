package com.ruoyi.harness.ai.provider;

import com.ruoyi.harness.ai.model.*;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class StructuredAiResponseParser {
    private final ObjectMapper mapper;
    public StructuredAiResponseParser(ObjectMapper mapper) { this.mapper = mapper; }

    public AiGenerationResult parse(String content, String model, String provider, Usage usage) {
        try {
            JsonNode root = mapper.readTree(content);
            if (root == null || !root.isObject()) return invalid("AI response must be a JSON object");
            String message = requiredText(root, "assistantMessage");
            String script = requiredText(root, "script");
            JsonNode used = root.get("capabilitiesUsed");
            if (used == null || !used.isArray()) return invalid("capabilitiesUsed must be an array");
            List<String> capabilities = new ArrayList<>();
            for (JsonNode item : used) {
                if (!item.isTextual() || item.asText().isBlank()) return invalid("capabilitiesUsed must contain names");
                capabilities.add(item.asText());
            }
            return new AiGenerationResult(message, script, capabilities.stream().distinct().toList(), model, provider, usage);
        } catch (HarnessAiException e) { throw e; }
        catch (Exception e) { throw new HarnessAiException(AiErrorCode.AI_RESPONSE_INVALID, "AI returned malformed structured output", e); }
    }
    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) return invalid(field + " is required");
        return value.asText();
    }
    private <T> T invalid(String message) { throw new HarnessAiException(AiErrorCode.AI_RESPONSE_INVALID, message); }
}
