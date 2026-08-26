package com.ruoyi.harness.api;

public record RuntimeLimits(long maxExecutionMillis, int maxCapabilityCalls, int maxSourceBytes,
        int maxInputBytes, int maxOutputBytes, int maxPageNodes, int maxLogEvents,
        int maxTableRowsInDefinition, int maxJsonDepth) {
    public static RuntimeLimits defaults() {
        return new RuntimeLimits(3000, 50, 524288, 262144, 1048576, 2000, 200, 1000, 64);
    }
}
