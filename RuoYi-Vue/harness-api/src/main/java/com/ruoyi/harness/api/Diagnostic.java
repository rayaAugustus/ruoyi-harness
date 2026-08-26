package com.ruoyi.harness.api;

public record Diagnostic(String severity, String code, String message, Integer line, Integer column) {
    public static Diagnostic error(HarnessErrorCode code, String message) {
        return new Diagnostic("error", code.name(), message, null, null);
    }
}
