package com.ruoyi.harness.core;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.core.domain.*;
import com.ruoyi.harness.core.port.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class HarnessRuntimeService {
    private final AppRegistryService appService; private final VersionService versionService;
    private final HarnessVersionRepository versions; private final HarnessScriptEngine engine; private final CapabilityInvoker capabilities;
    private final AppAccessEvaluator access; private final ExecutionLogRepository logs; private final PublishedArtifactCache cache; private final ObjectMapper mapper;
    public HarnessRuntimeService(AppRegistryService a,VersionService v,HarnessVersionRepository vr,HarnessScriptEngine e,
            CapabilityInvoker c,AppAccessEvaluator access,ExecutionLogRepository logs,PublishedArtifactCache cache,ObjectMapper mapper){
        appService=a;versionService=v;versions=vr;engine=e;capabilities=c;this.access=access;this.logs=logs;this.cache=cache;this.mapper=mapper;}

    public RuntimeDescriptor resolve(String appKey,RuntimeIdentity identity){HarnessApp app=authorizedApp(appKey,identity); HarnessAppVersion v=published(app);
        return new RuntimeDescriptor(app.getAppKey(),app.getName(),v.getId(),v.getVersionNo(),v.getSdkVersion(),true,etag(v));}
    public RuntimeResponse render(String appKey,RenderRequest request,RuntimeIdentity identity,String requestId,String locale){
        HarnessApp app=authorizedApp(appKey,identity); HarnessAppVersion v=published(app); ObjectNode input=mapper.createObjectNode();
        input.set("route",request==null||request.route()==null?mapper.createObjectNode():request.route()); input.set("state",request==null||request.state()==null?mapper.createObjectNode():request.state());
        ExecutionOutcome outcome=execute(app,v,"PAGE","page",input,mapper.createObjectNode(),identity,requestId,locale);
        return new RuntimeResponse(appKey,v.getId(),outcome.traceId(),outcome.result().value(),null);
    }
    public RuntimeResponse action(String appKey,String actionName,ActionRequest request,RuntimeIdentity identity,String requestId,String locale){
        HarnessApp app=authorizedApp(appKey,identity); if(request==null||request.versionId()==null)throw new HarnessException(HarnessErrorCode.APP_VERSION_NOT_FOUND,"versionId is required");
        HarnessAppVersion v=versionService.requireEntity(app,request.versionId());
        if(v.getStatus()!=VersionStatus.PUBLISHED&&v.getStatus()!=VersionStatus.SUPERSEDED)throw new HarnessException(HarnessErrorCode.APP_VERSION_NOT_FOUND,"Pinned version is not executable");
        ExecutionOutcome outcome=execute(app,v,"ACTION",actionName,request.input(),request.clientState(),identity,requestId,locale);
        ScriptExecutionResult result=outcome.result();
        JsonNode page="page".equals(result.value().path("type").asText())?result.value():null; JsonNode effects=page==null?result.value():null;
        return new RuntimeResponse(appKey,v.getId(),outcome.traceId(),page,effects);
    }
    private ExecutionOutcome execute(HarnessApp app,HarnessAppVersion version,String entryType,String entryName,JsonNode input,JsonNode state,
            RuntimeIdentity identity,String requestId,String locale){String traceId=UUID.randomUUID().toString();Instant started=Instant.now();
        ScriptExecutionResult result=null; HarnessException failure=null;
        try{ScriptArtifact artifact=cachedArtifact(app,version);verifyHash(artifact); result=engine.execute(artifact,new ScriptExecutionContext(entryType,entryName,input,state,identity,locale,
                requestId==null?UUID.randomUUID().toString():requestId,traceId,capabilities));
            if(!result.success()){HarnessErrorCode code=HarnessErrorCode.valueOf(result.errorCode());throw new HarnessException(code,result.diagnostics().isEmpty()?"Script execution failed":result.diagnostics().get(0).message());}
            return new ExecutionOutcome(traceId,result);
        }catch(HarnessException e){failure=e;throw e;}catch(RuntimeException e){failure=new HarnessException(HarnessErrorCode.SCRIPT_RUNTIME_ERROR,"Script execution failed",e);throw failure;}finally{ExecutionLog log=new ExecutionLog();log.setTraceId(traceId);log.setRequestId(requestId);log.setAppId(app.getId());log.setAppVersionId(version.getId());log.setUserId(identity.userId());
            log.setEntryType(entryType);log.setEntryName(entryName);log.setStartedAt(started);log.setFinishedAt(Instant.now());log.setElapsedMs(java.time.Duration.between(started,log.getFinishedAt()).toMillis());
            log.setCapabilityCalls(result==null?0:result.capabilityCalls());log.setStatus(failure==null?"SUCCESS":"ERROR");if(failure!=null){log.setErrorCode(failure.getCode().name());log.setErrorSummary(failure.getMessage());}logs.insert(log);}
    }
    private HarnessApp authorizedApp(String appKey,RuntimeIdentity identity){HarnessApp app=appService.requireEntity(appKey);if(!Boolean.TRUE.equals(app.getEnabled()))throw new HarnessException(HarnessErrorCode.APP_DISABLED,"Application is disabled");
        if(!access.isAllowed(app.getRequiredPermission(),identity))throw new HarnessException(HarnessErrorCode.APP_ACCESS_DENIED,"Application access denied");return app;}
    private HarnessAppVersion published(HarnessApp app){if(app.getPublishedVersionId()==null)throw new HarnessException(HarnessErrorCode.APP_VERSION_NOT_FOUND,"Application has no published version");return versionService.requireEntity(app,app.getPublishedVersionId());}
    private ScriptArtifact cachedArtifact(HarnessApp app,HarnessAppVersion version){ScriptArtifact artifact=cache.get(app.getAppKey(),version.getId());if(artifact==null){artifact=VersionService.artifact(app,version);cache.put(artifact);}return artifact;}
    private static void verifyHash(ScriptArtifact artifact){try{String actual="sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(artifact.source().getBytes(StandardCharsets.UTF_8)));if(artifact.sourceHash()==null||!actual.equals(artifact.sourceHash()))throw new HarnessException(HarnessErrorCode.SCRIPT_VALIDATION_ERROR,"Published source integrity check failed");}catch(HarnessException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String etag(HarnessAppVersion v){return "\""+v.getId()+":"+v.getSourceHash()+"\"";}
    private record ExecutionOutcome(String traceId,ScriptExecutionResult result){}
    public record RuntimeDescriptor(String appKey,String name,Long versionId,Long version,String sdkVersion,boolean enabled,String etag){}
}
