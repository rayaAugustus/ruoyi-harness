package com.ruoyi.harness.adapter.audit;

import com.ruoyi.harness.capability.*;
import com.ruoyi.harness.core.domain.CapabilityLog;
import com.ruoyi.harness.core.port.CapabilityLogRepository;
import java.time.Instant;

public class RuoYiCapabilityAuditSink implements CapabilityAuditSink {
    private final CapabilityLogRepository repository; public RuoYiCapabilityAuditSink(CapabilityLogRepository r){repository=r;}
    @Override public void record(CapabilityAuditEvent e){CapabilityLog log=new CapabilityLog();log.setTraceId(e.traceId());log.setCapabilityName(e.capabilityName());log.setCapabilityVersion(e.capabilityVersion());
        log.setUserId(e.userId());log.setRiskLevel(e.riskLevel().name());log.setStatus(e.status());log.setElapsedMs(e.elapsedMillis());log.setErrorCode(e.errorCode());log.setCreatedAt(Instant.now());repository.insert(log);}
}
