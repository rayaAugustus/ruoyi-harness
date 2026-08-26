package com.ruoyi.harness.capability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Small deny-by-default JSON Schema subset used at capability boundaries. */
public final class JsonSchemaValidator {
    public List<String> validate(JsonNode schema, JsonNode value) {
        List<String> errors = new ArrayList<>();
        if (schema != null && !schema.isNull()) validateAt(schema, value, "$", errors);
        return errors;
    }

    private void validateAt(JsonNode schema, JsonNode value, String path, List<String> errors) {
        JsonNode type = schema.get("type");
        if (type != null && !matches(type.asText(), value)) {
            errors.add(path + " must be " + type.asText()); return;
        }
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && enumNode.isArray()) {
            boolean found = false;
            for (JsonNode candidate : enumNode) if (candidate.equals(value)) { found = true; break; }
            if (!found) errors.add(path + " is not an allowed value");
        }
        if (value != null && value.isObject()) validateObject(schema, value, path, errors);
        if (value != null && value.isArray()) {
            JsonNode items = schema.get("items");
            if (items != null) for (int i = 0; i < value.size(); i++) validateAt(items, value.get(i), path + "[" + i + "]", errors);
            limit(schema, "maxItems", value.size(), path, errors);
        }
        if (value != null && value.isTextual()) limit(schema, "maxLength", value.asText().length(), path, errors);
    }

    private void validateObject(JsonNode schema, JsonNode value, String path, List<String> errors) {
        JsonNode required = schema.get("required");
        if (required != null) for (JsonNode name : required) if (!value.has(name.asText()))
            errors.add(path + "." + name.asText() + " is required");
        JsonNode properties = schema.get("properties");
        Set<String> allowed = new HashSet<>();
        if (properties != null) {
            for (String name : properties.propertyNames()) {
                allowed.add(name);
                if (value.has(name)) validateAt(properties.get(name), value.get(name), path + "." + name, errors);
            }
        }
        JsonNode additional = schema.get("additionalProperties");
        if (additional != null && !additional.asBoolean(true)) {
            for (String name : value.propertyNames()) if (!allowed.contains(name)) errors.add(path + "." + name + " is not allowed");
        }
    }

    private static boolean matches(String type, JsonNode value) {
        if (value == null || value.isNull()) return "null".equals(type);
        return switch (type) {
            case "object" -> value.isObject(); case "array" -> value.isArray();
            case "string" -> value.isTextual(); case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber(); case "boolean" -> value.isBoolean();
            default -> false;
        };
    }

    private static void limit(JsonNode schema, String field, int actual, String path, List<String> errors) {
        JsonNode limit = schema.get(field);
        if (limit != null && actual > limit.asInt()) errors.add(path + " exceeds " + field + " " + limit.asInt());
    }
}
