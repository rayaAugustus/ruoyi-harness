package com.ruoyi.harness.api;

public interface HarnessScriptEngine {
    ValidationResult validate(ScriptArtifact artifact);
    ScriptExecutionResult execute(ScriptArtifact artifact, ScriptExecutionContext context);
}
