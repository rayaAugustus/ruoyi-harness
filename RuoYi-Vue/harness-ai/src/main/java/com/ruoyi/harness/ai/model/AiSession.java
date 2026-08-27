package com.ruoyi.harness.ai.model;

import java.time.Instant;

public class AiSession {
    private Long id; private String sessionKey; private Long appId; private Long activeVersionId;
    private Long createdBy; private Instant createdAt; private Instant updatedAt; private String title; private String status;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getSessionKey(){return sessionKey;} public void setSessionKey(String v){sessionKey=v;}
    public Long getAppId(){return appId;} public void setAppId(Long v){appId=v;} public Long getActiveVersionId(){return activeVersionId;} public void setActiveVersionId(Long v){activeVersionId=v;}
    public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
