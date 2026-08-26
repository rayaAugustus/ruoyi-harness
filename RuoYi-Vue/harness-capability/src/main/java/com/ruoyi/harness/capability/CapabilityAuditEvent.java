package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.RiskLevel;

public record CapabilityAuditEvent(String traceId, String capabilityName, String capabilityVersion,
        Long userId, RiskLevel riskLevel, String status, long elapsedMillis, String errorCode) {}
