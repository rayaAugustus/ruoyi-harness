package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.CapabilityContext;
import com.ruoyi.harness.api.CapabilityDefinition;
import com.ruoyi.harness.api.CapabilityInvoker;
import com.ruoyi.harness.api.HarnessErrorCode;
import com.ruoyi.harness.api.HarnessException;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class CapabilityBridge implements CapabilityInvoker {
    private final CapabilityRegistry registry;
    private final JsonSchemaValidator schemas;
    private final PermissionEvaluator permissions;
    private final CapabilityPolicy policy;
    private final CapabilityAuditSink audit;

    public CapabilityBridge(CapabilityRegistry registry, JsonSchemaValidator schemas,
            PermissionEvaluator permissions, CapabilityPolicy policy, CapabilityAuditSink audit) {
        this.registry = registry; this.schemas = schemas; this.permissions = permissions;
        this.policy = policy; this.audit = audit;
    }

    @Override
    public JsonNode invoke(String name, JsonNode input, CapabilityContext context) {
        CapabilityDefinition definition = registry.requireLatest(name);
        long started = System.nanoTime(); String status = "SUCCESS"; String errorCode = null;
        try {
            List<String> inputErrors = schemas.validate(definition.inputSchema(), input);
            if (!inputErrors.isEmpty()) throw new HarnessException(HarnessErrorCode.CAPABILITY_INPUT_INVALID,
                    "Capability input is invalid", inputErrors, null);
            if (definition.requiredPermission() != null && !definition.requiredPermission().isBlank()
                    && !permissions.isAllowed(definition.requiredPermission(), context)) {
                throw new HarnessException(HarnessErrorCode.CAPABILITY_PERMISSION_DENIED, "Permission denied");
            }
            if (!policy.isAllowed(definition, input, context)) {
                throw new HarnessException(HarnessErrorCode.CAPABILITY_POLICY_DENIED, "Capability denied by policy");
            }
            JsonNode output;
            try { output = definition.handler().handle(input, context); }
            catch (HarnessException e) { throw e; }
            catch (Exception e) { throw new HarnessException(HarnessErrorCode.CAPABILITY_EXECUTION_ERROR,
                    "Capability execution failed", e); }
            List<String> outputErrors = schemas.validate(definition.outputSchema(), output);
            if (!outputErrors.isEmpty()) throw new HarnessException(HarnessErrorCode.CAPABILITY_OUTPUT_INVALID,
                    "Capability output is invalid", outputErrors, null);
            return output;
        } catch (HarnessException e) {
            status = "DENIED";
            if (e.getCode() == HarnessErrorCode.CAPABILITY_EXECUTION_ERROR) status = "ERROR";
            errorCode = e.getCode().name(); throw e;
        } finally {
            audit.record(new CapabilityAuditEvent(context.traceId(), definition.name(), definition.version(),
                    context.userId(), definition.riskLevel(), status,
                    (System.nanoTime() - started) / 1_000_000, errorCode));
        }
    }
}
