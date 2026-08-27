package com.ruoyi.harness.ai.service;

import com.ruoyi.harness.ai.model.*;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.capability.CapabilityRegistry;
import com.ruoyi.harness.capability.ValidationCapabilityInvoker;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import tools.jackson.databind.ObjectMapper;

public final class AiGenerationValidator {
    private static final Pattern CALL=Pattern.compile("\\bharness\\s*\\.\\s*call\\s*\\(\\s*['\"]([^'\"]+)['\"]");
    private final HarnessScriptEngine engine;private final CapabilityRegistry registry;private final ValidationCapabilityInvoker validationCapabilities;private final ObjectMapper mapper;private final int maxSourceBytes;
    public AiGenerationValidator(HarnessScriptEngine engine,CapabilityRegistry registry,ValidationCapabilityInvoker validationCapabilities,ObjectMapper mapper,int maxSourceBytes){this.engine=engine;this.registry=registry;this.validationCapabilities=validationCapabilities;this.mapper=mapper;this.maxSourceBytes=maxSourceBytes;}

    public ValidationResult validate(String appKey,String script,List<String> declared){
        if(script==null||script.isBlank())throw new HarnessAiException(AiErrorCode.AI_RESPONSE_INVALID,"Generated script is missing");
        if(script.getBytes(StandardCharsets.UTF_8).length>maxSourceBytes)throw new HarnessAiException(AiErrorCode.AI_SCRIPT_INVALID,"Generated script exceeds the runtime source limit");
        Set<String> known=new HashSet<>();registry.list().forEach(d->known.add(d.name()));
        Set<String> referenced=new LinkedHashSet<>(declared==null?List.of():declared);Matcher matcher=CALL.matcher(script);while(matcher.find())referenced.add(matcher.group(1));
        List<String> unknown=referenced.stream().filter(name->!known.contains(name)).toList();
        if(!unknown.isEmpty())throw new HarnessAiException(AiErrorCode.AI_CAPABILITY_UNKNOWN,"AI declared or used unavailable capabilities",unknown,null);
        ScriptArtifact artifact=new ScriptArtifact(0L,0L,appKey,0L,"1",script,null,VersionStatus.DRAFT);ValidationResult result=engine.validate(artifact);
        if(result.valid()){ScriptExecutionContext context=new ScriptExecutionContext("PAGE","page",mapper.createObjectNode(),mapper.createObjectNode(),new RuntimeIdentity(0L,"validation",Set.of()),"en","ai-validation","ai-validation",validationCapabilities);ScriptExecutionResult runtime=engine.execute(artifact,context);if(!runtime.success())result=new ValidationResult(false,runtime.diagnostics(),result.sourceHash());}
        return result;
    }
    public List<String> inspectCapabilities(String script){LinkedHashSet<String> result=new LinkedHashSet<>();Matcher matcher=CALL.matcher(script==null?"":script);while(matcher.find())result.add(matcher.group(1));return List.copyOf(result);}
}
