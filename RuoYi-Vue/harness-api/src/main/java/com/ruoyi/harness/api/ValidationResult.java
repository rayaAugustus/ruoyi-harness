package com.ruoyi.harness.api;

import java.util.List;

public record ValidationResult(boolean valid, List<Diagnostic> diagnostics, String sourceHash) {
    public ValidationResult { diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics); }
}
