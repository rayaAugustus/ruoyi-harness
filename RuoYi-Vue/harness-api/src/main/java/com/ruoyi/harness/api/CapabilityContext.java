package com.ruoyi.harness.api;

import java.util.Set;

public record CapabilityContext(Long userId, String username, Set<String> permissions,
        String appKey, Long appVersionId, String requestId, String traceId) {}
