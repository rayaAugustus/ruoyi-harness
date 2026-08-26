package com.ruoyi.harness.api;

public record HarnessError(String code, String message, String traceId, Object details) {}
