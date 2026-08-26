package com.ruoyi.harness.runtime;

import com.ruoyi.harness.api.HarnessErrorCode;
import com.ruoyi.harness.api.HarnessException;
import com.ruoyi.harness.api.RuntimeLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public final class UiDefinitionValidator {
    private static final Map<String, Set<String>> FIELDS = Map.ofEntries(
        Map.entry("page", Set.of("type", "id", "title", "children")),
        Map.entry("section", Set.of("type", "id", "title", "children")),
        Map.entry("text", Set.of("type", "id", "value", "variant")),
        Map.entry("statistic", Set.of("type", "id", "label", "value", "suffix")),
        Map.entry("table", Set.of("type", "id", "columns", "rows", "emptyText")),
        Map.entry("form", Set.of("type", "id", "fields", "children")),
        Map.entry("input", Set.of("type", "id", "name", "label", "value", "placeholder", "required", "inputType")),
        Map.entry("select", Set.of("type", "id", "name", "label", "value", "options", "required")),
        Map.entry("button", Set.of("type", "id", "text", "variant", "action", "disabled")),
        Map.entry("tabs", Set.of("type", "id", "items")),
        Map.entry("modal", Set.of("type", "id", "title", "open", "children")),
        Map.entry("alert", Set.of("type", "id", "title", "message", "variant")),
        Map.entry("chart", Set.of("type", "id", "chartType", "title", "labels", "series")));
    private final RuntimeLimits limits;
    public UiDefinitionValidator(RuntimeLimits limits) { this.limits = limits; }

    public void validatePage(JsonNode root) {
        List<String> errors = new ArrayList<>(); int[] count = {0};
        validate(root, "$", count, errors);
        if (root == null || !"page".equals(root.path("type").asText())) errors.add("$ must be a page component");
        if (!errors.isEmpty()) throw new HarnessException(HarnessErrorCode.UI_SCHEMA_INVALID,
                "Invalid UI definition", errors, null);
    }

    private void validate(JsonNode node, String path, int[] count, List<String> errors) {
        if (node == null || !node.isObject()) { errors.add(path + " must be an object"); return; }
        if (++count[0] > limits.maxPageNodes()) { errors.add("page exceeds node limit"); return; }
        String type = node.path("type").asText(null);
        Set<String> fields = FIELDS.get(type);
        if (fields == null) { errors.add(path + " has unsupported component type: " + type); return; }
        for (String name : node.propertyNames()) if (!fields.contains(name)) errors.add(path + "." + name + " is not allowed");
        if (("page".equals(type) || "section".equals(type)) && !node.path("children").isArray()) errors.add(path + ".children must be an array");
        if ("table".equals(type)) {
            if (!node.path("columns").isArray() || !node.path("rows").isArray()) errors.add(path + " requires columns and rows arrays");
            if (node.path("rows").size() > limits.maxTableRowsInDefinition()) errors.add(path + ".rows exceeds limit");
            for (JsonNode column : node.path("columns")) if (!column.isObject() || !column.hasNonNull("key") || !column.hasNonNull("label")) errors.add(path + ".columns entries require key and label");
        }
        if ("button".equals(type)) {
            JsonNode action = node.get("action");
            if (action != null && (!action.isObject() || !action.hasNonNull("name"))) errors.add(path + ".action requires name");
        }
        visit(node.get("children"), path + ".children", count, errors);
        visit(node.get("fields"), path + ".fields", count, errors);
        JsonNode items = node.get("items");
        if (items != null && items.isArray()) for (int i = 0; i < items.size(); i++) visit(items.get(i).get("children"), path + ".items[" + i + "].children", count, errors);
    }

    private void visit(JsonNode children, String path, int[] count, List<String> errors) {
        if (children == null) return;
        if (!children.isArray()) { errors.add(path + " must be an array"); return; }
        for (int i = 0; i < children.size(); i++) validate(children.get(i), path + "[" + i + "]", count, errors);
    }
}
