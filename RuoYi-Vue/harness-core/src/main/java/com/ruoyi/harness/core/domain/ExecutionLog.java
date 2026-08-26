package com.ruoyi.harness.core.domain;

import java.time.Instant;

public class ExecutionLog {
    private Long id; private String traceId; private String requestId; private Long appId; private Long appVersionId;
    private Long userId; private String entryType; private String entryName; private String status; private Instant startedAt;
    private Instant finishedAt; private Long elapsedMs; private Integer capabilityCalls; private String errorCode; private String errorSummary;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getTraceId(){return traceId;} public void setTraceId(String v){traceId=v;}
    public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=v;} public Long getAppId(){return appId;} public void setAppId(Long v){appId=v;}
    public Long getAppVersionId(){return appVersionId;} public void setAppVersionId(Long v){appVersionId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getEntryType(){return entryType;} public void setEntryType(String v){entryType=v;} public String getEntryName(){return entryName;} public void setEntryName(String v){entryName=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;} public Instant getStartedAt(){return startedAt;} public void setStartedAt(Instant v){startedAt=v;}
    public Instant getFinishedAt(){return finishedAt;} public void setFinishedAt(Instant v){finishedAt=v;} public Long getElapsedMs(){return elapsedMs;} public void setElapsedMs(Long v){elapsedMs=v;}
    public Integer getCapabilityCalls(){return capabilityCalls;} public void setCapabilityCalls(Integer v){capabilityCalls=v;} public String getErrorCode(){return errorCode;} public void setErrorCode(String v){errorCode=v;}
    public String getErrorSummary(){return errorSummary;} public void setErrorSummary(String v){errorSummary=v;}
}
