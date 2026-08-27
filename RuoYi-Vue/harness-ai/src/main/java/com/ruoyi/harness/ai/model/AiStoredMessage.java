package com.ruoyi.harness.ai.model;

import java.time.Instant;

public class AiStoredMessage {
    private Long id; private Long sessionId; private String role; private String content; private String scriptSnapshot;
    private String model; private String provider; private Long inputTokens; private Long outputTokens; private Instant createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;} public String getContent(){return content;} public void setContent(String v){content=v;}
    public String getScriptSnapshot(){return scriptSnapshot;} public void setScriptSnapshot(String v){scriptSnapshot=v;} public String getModel(){return model;} public void setModel(String v){model=v;}
    public String getProvider(){return provider;} public void setProvider(String v){provider=v;} public Long getInputTokens(){return inputTokens;} public void setInputTokens(Long v){inputTokens=v;}
    public Long getOutputTokens(){return outputTokens;} public void setOutputTokens(Long v){outputTokens=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
