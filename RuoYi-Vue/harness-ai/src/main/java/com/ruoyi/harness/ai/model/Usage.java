package com.ruoyi.harness.ai.model;

public record Usage(Long inputTokens, Long outputTokens) {
    public static Usage empty() { return new Usage(null, null); }
}
