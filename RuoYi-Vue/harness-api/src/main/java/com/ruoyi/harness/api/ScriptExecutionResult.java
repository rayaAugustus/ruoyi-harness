package com.ruoyi.harness.api;

import java.util.List;
import tools.jackson.databind.JsonNode;

public record ScriptExecutionResult(boolean success, JsonNode value, List<Diagnostic> diagnostics,
        String errorCode, int capabilityCalls, int logEvents, long elapsedMillis) {
    public ScriptExecutionResult { diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics); }
}
