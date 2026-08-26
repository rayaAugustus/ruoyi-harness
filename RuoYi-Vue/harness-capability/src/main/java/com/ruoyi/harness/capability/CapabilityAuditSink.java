package com.ruoyi.harness.capability;

@FunctionalInterface
public interface CapabilityAuditSink {
    void record(CapabilityAuditEvent event);
    static CapabilityAuditSink noop() { return event -> { }; }
}
