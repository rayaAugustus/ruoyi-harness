package com.ruoyi.harness.core.domain;

import com.ruoyi.harness.api.VersionStatus;
import java.time.Instant;

public class HarnessAppVersion {
    private Long id; private Long appId; private Long versionNo; private String sdkVersion; private String source;
    private String sourceHash; private VersionStatus status; private Long createdBy; private Instant createdAt;
    private Instant validatedAt; private Instant publishedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getAppId(){return appId;} public void setAppId(Long v){appId=v;}
    public Long getVersionNo(){return versionNo;} public void setVersionNo(Long v){versionNo=v;} public String getSdkVersion(){return sdkVersion;} public void setSdkVersion(String v){sdkVersion=v;}
    public String getSource(){return source;} public void setSource(String v){source=v;} public String getSourceHash(){return sourceHash;} public void setSourceHash(String v){sourceHash=v;}
    public VersionStatus getStatus(){return status;} public void setStatus(VersionStatus v){status=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getValidatedAt(){return validatedAt;} public void setValidatedAt(Instant v){validatedAt=v;}
    public Instant getPublishedAt(){return publishedAt;} public void setPublishedAt(Instant v){publishedAt=v;}
}
