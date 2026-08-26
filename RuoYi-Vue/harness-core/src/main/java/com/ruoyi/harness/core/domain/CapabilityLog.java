package com.ruoyi.harness.core.domain;

import java.time.Instant;

public class CapabilityLog {
    private Long id; private String traceId; private Long executionLogId; private String capabilityName;
    private String capabilityVersion; private Long userId; private String riskLevel; private String status;
    private Long elapsedMs; private String errorCode; private Instant createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTraceId(){return traceId;} public void setTraceId(String v){traceId=v;}
    public Long getExecutionLogId(){return executionLogId;} public void setExecutionLogId(Long v){executionLogId=v;} public String getCapabilityName(){return capabilityName;} public void setCapabilityName(String v){capabilityName=v;}
    public String getCapabilityVersion(){return capabilityVersion;} public void setCapabilityVersion(String v){capabilityVersion=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Long getElapsedMs(){return elapsedMs;} public void setElapsedMs(Long v){elapsedMs=v;} public String getErrorCode(){return errorCode;} public void setErrorCode(String v){errorCode=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
