package com.ruoyi.harness.ai.service;

import com.ruoyi.harness.ai.model.AiErrorCode;
import com.ruoyi.harness.ai.model.HarnessAiException;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.capability.*;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class AiPreviewService {
    private final HarnessScriptEngine engine;private final CapabilityRegistry registry;private final JsonSchemaValidator schemas;
    private final PermissionEvaluator permissions;private final CapabilityAuditSink audit;private final ObjectMapper mapper;
    public AiPreviewService(HarnessScriptEngine engine,CapabilityRegistry registry,JsonSchemaValidator schemas,
            PermissionEvaluator permissions,CapabilityAuditSink audit,ObjectMapper mapper){this.engine=engine;this.registry=registry;this.schemas=schemas;this.permissions=permissions;this.audit=audit;this.mapper=mapper;}

    public RuntimeResponse preview(AppDescriptor app,AppVersionDescriptor version,RuntimeIdentity identity,AiPreviewRequest request){
        if(version.status()!=VersionStatus.VALIDATED)throw new HarnessAiException(AiErrorCode.AI_SCRIPT_INVALID,"Preview requires a validated draft");
        if(request!=null&&request.versionId()!=null&&!request.versionId().equals(version.id()))throw new HarnessAiException(AiErrorCode.AI_DRAFT_NOT_FOUND,"Preview version is no longer active");
        String traceId=UUID.randomUUID().toString();String requestId=UUID.randomUUID().toString();
        CapabilityBridge bridge=new CapabilityBridge(registry,schemas,permissions,(definition,input,context)->definition.riskLevel()==RiskLevel.READ,audit);
        boolean action=request!=null&&request.actionName()!=null&&!request.actionName().isBlank();ObjectNode input=mapper.createObjectNode();JsonNode state=request==null||request.state()==null?mapper.createObjectNode():request.state();
        if(action)input.setAll(asObject(request.input()));else{input.set("route",request==null||request.route()==null?mapper.createObjectNode():request.route());input.set("state",state);}
        ScriptArtifact artifact=new ScriptArtifact(version.id(),app.id(),app.appKey(),version.versionNo(),version.sdkVersion(),version.source(),version.sourceHash(),version.status());
        ScriptExecutionResult result=engine.execute(artifact,new ScriptExecutionContext(action?"ACTION":"PAGE",action?request.actionName():"page",input,state,identity,"en",requestId,traceId,bridge));
        if(!result.success()){HarnessErrorCode code;try{code=HarnessErrorCode.valueOf(result.errorCode());}catch(Exception ignored){code=HarnessErrorCode.SCRIPT_RUNTIME_ERROR;}throw new HarnessException(code,result.diagnostics().isEmpty()?"Preview failed":result.diagnostics().get(0).message(),result.diagnostics(),null);}
        JsonNode page=!action||"page".equals(result.value().path("type").asText())?result.value():null;JsonNode effects=page==null?result.value():null;
        return new RuntimeResponse(app.appKey(),version.id(),traceId,page,effects);
    }
    private ObjectNode asObject(JsonNode value){return value!=null&&value.isObject()?(ObjectNode)value:mapper.createObjectNode();}
    public record AiPreviewRequest(Long versionId,String actionName,JsonNode route,JsonNode input,JsonNode state) {}
}
