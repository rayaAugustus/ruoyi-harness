package com.ruoyi.harness.core;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.capability.ValidationCapabilityInvoker;
import com.ruoyi.harness.core.domain.HarnessApp;
import com.ruoyi.harness.core.domain.HarnessAppVersion;
import com.ruoyi.harness.core.port.HarnessVersionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

public class VersionService {
    private final AppRegistryService appService; private final HarnessVersionRepository versions;
    private final HarnessScriptEngine engine; private final ValidationCapabilityInvoker validationCapabilities;
    private final ObjectMapper mapper;
    public VersionService(AppRegistryService appService, HarnessVersionRepository versions, HarnessScriptEngine engine,
            ValidationCapabilityInvoker validationCapabilities, ObjectMapper mapper) {
        this.appService=appService; this.versions=versions; this.engine=engine; this.validationCapabilities=validationCapabilities; this.mapper=mapper;
    }
    public List<AppVersionDescriptor> list(String appKey) { HarnessApp app=appService.requireEntity(appKey); return versions.findByAppId(app.getId()).stream().map(VersionService::descriptor).toList(); }
    public AppVersionDescriptor require(String appKey, Long id) { return descriptor(requireEntity(appService.requireEntity(appKey), id)); }

    @Transactional public AppVersionDescriptor create(String appKey, String sdkVersion, String source, Long actorId) {
        HarnessApp app=appService.requireEntity(appKey); HarnessAppVersion v=new HarnessAppVersion();
        v.setAppId(app.getId()); v.setVersionNo(versions.nextVersionNo(app.getId())); v.setSdkVersion(sdkVersion == null ? "1" : sdkVersion);
        v.setSource(source == null ? "" : source); v.setStatus(VersionStatus.DRAFT); v.setCreatedBy(actorId); v.setCreatedAt(Instant.now());
        versions.insert(v); return descriptor(v);
    }
    @Transactional public AppVersionDescriptor updateSource(String appKey, Long id, String source) {
        HarnessApp app=appService.requireEntity(appKey); HarnessAppVersion v=requireEntity(app,id);
        if (v.getStatus()==VersionStatus.PUBLISHED || v.getStatus()==VersionStatus.SUPERSEDED)
            throw new HarnessException(HarnessErrorCode.VERSION_IMMUTABLE, "Published versions are immutable");
        versions.updateDraftSource(id, source == null ? "" : source, VersionStatus.DRAFT);
        v.setSource(source); v.setSourceHash(null); v.setValidatedAt(null); v.setStatus(VersionStatus.DRAFT); return descriptor(v);
    }
    @Transactional public ValidationResult validate(String appKey, Long id) {
        HarnessApp app=appService.requireEntity(appKey); HarnessAppVersion v=requireEntity(app,id);
        if (v.getStatus()==VersionStatus.PUBLISHED || v.getStatus()==VersionStatus.SUPERSEDED)
            throw new HarnessException(HarnessErrorCode.VERSION_IMMUTABLE, "Published versions are immutable");
        ScriptArtifact artifact=artifact(app,v); ValidationResult result=engine.validate(artifact);
        if (result.valid()) {
            ScriptExecutionContext ctx=new ScriptExecutionContext("PAGE","page",mapper.createObjectNode(),mapper.createObjectNode(),
                    new RuntimeIdentity(0L,"validation",java.util.Set.of()),"en","validation","validation",validationCapabilities);
            ScriptExecutionResult runtime=engine.execute(artifact,ctx);
            if (!runtime.success()) result=new ValidationResult(false,runtime.diagnostics(),result.sourceHash());
        }
        VersionStatus status=result.valid()?VersionStatus.VALIDATED:VersionStatus.REJECTED;
        versions.updateValidation(id,result.sourceHash(),status,Instant.now()); return result;
    }
    @Transactional public void deleteDraft(String appKey, Long id) {
        HarnessApp app=appService.requireEntity(appKey); HarnessAppVersion v=requireEntity(app,id);
        if (v.getStatus()!=VersionStatus.DRAFT && v.getStatus()!=VersionStatus.REJECTED) throw new HarnessException(HarnessErrorCode.VERSION_STATE_INVALID,"Only unused drafts can be deleted");
        versions.deleteDraft(id);
    }
    HarnessAppVersion requireEntity(HarnessApp app, Long id) {
        HarnessAppVersion v=versions.findById(id);
        if (v==null) throw new HarnessException(HarnessErrorCode.APP_VERSION_NOT_FOUND,"Version not found");
        if (!app.getId().equals(v.getAppId())) throw new HarnessException(HarnessErrorCode.VERSION_OWNERSHIP_INVALID,"Version does not belong to application");
        return v;
    }
    static ScriptArtifact artifact(HarnessApp app,HarnessAppVersion v){return new ScriptArtifact(v.getId(),v.getAppId(),app.getAppKey(),v.getVersionNo(),v.getSdkVersion(),v.getSource(),v.getSourceHash(),v.getStatus());}
    static AppVersionDescriptor descriptor(HarnessAppVersion v){return new AppVersionDescriptor(v.getId(),v.getAppId(),v.getVersionNo(),v.getSdkVersion(),v.getSource(),v.getSourceHash(),v.getStatus(),v.getCreatedBy(),v.getCreatedAt(),v.getValidatedAt(),v.getPublishedAt());}
}
