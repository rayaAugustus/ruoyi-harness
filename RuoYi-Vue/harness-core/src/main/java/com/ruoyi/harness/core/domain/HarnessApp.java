package com.ruoyi.harness.core.domain;

import java.time.Instant;

public class HarnessApp {
    private Long id; private String appKey; private String name; private String description;
    private String routeTitle; private String icon; private Integer orderNum; private String requiredPermission;
    private Boolean enabled; private Long publishedVersionId; private Long createdBy; private Instant createdAt;
    private Long updatedBy; private Instant updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getAppKey(){return appKey;} public void setAppKey(String v){appKey=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getRouteTitle(){return routeTitle;} public void setRouteTitle(String v){routeTitle=v;} public String getIcon(){return icon;} public void setIcon(String v){icon=v;}
    public Integer getOrderNum(){return orderNum;} public void setOrderNum(Integer v){orderNum=v;} public String getRequiredPermission(){return requiredPermission;} public void setRequiredPermission(String v){requiredPermission=v;}
    public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;} public Long getPublishedVersionId(){return publishedVersionId;} public void setPublishedVersionId(Long v){publishedVersionId=v;}
    public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Long getUpdatedBy(){return updatedBy;} public void setUpdatedBy(Long v){updatedBy=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
