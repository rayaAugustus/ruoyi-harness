package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.*;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Validates calls and returns schema-shaped data without invoking business handlers. */
public final class ValidationCapabilityInvoker implements CapabilityInvoker {
    private final CapabilityRegistry registry; private final JsonSchemaValidator validator; private final ObjectMapper mapper;
    public ValidationCapabilityInvoker(CapabilityRegistry registry, JsonSchemaValidator validator, ObjectMapper mapper) {
        this.registry = registry; this.validator = validator; this.mapper = mapper;
    }
    @Override public JsonNode invoke(String name, JsonNode input, CapabilityContext context) {
        CapabilityDefinition definition = registry.requireLatest(name);
        List<String> errors = validator.validate(definition.inputSchema(), input);
        if (!errors.isEmpty()) throw new HarnessException(HarnessErrorCode.CAPABILITY_INPUT_INVALID, "Capability input is invalid", errors, null);
        return sample(definition.outputSchema());
    }
    private JsonNode sample(JsonNode schema) {
        if (schema == null || schema.isNull()) return mapper.createObjectNode();
        JsonNode example = schema.get("example"); if (example != null) return example.deepCopy();
        String type = schema.path("type").asText("object");
        return switch (type) {
            case "array" -> { ArrayNode a = mapper.createArrayNode(); yield a; }
            case "string" -> mapper.getNodeFactory().textNode("");
            case "integer" -> mapper.getNodeFactory().numberNode(0);
            case "number" -> mapper.getNodeFactory().numberNode(0.0);
            case "boolean" -> mapper.getNodeFactory().booleanNode(false);
            case "null" -> mapper.nullNode();
            default -> { ObjectNode object = mapper.createObjectNode(); JsonNode properties = schema.get("properties");
                if (properties != null) for (String n : properties.propertyNames()) object.set(n, sample(properties.get(n))); yield object; }
        };
    }
}
