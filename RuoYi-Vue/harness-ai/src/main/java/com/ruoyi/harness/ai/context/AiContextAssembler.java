package com.ruoyi.harness.ai.context;

import com.ruoyi.harness.ai.model.*;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.capability.CapabilityRegistry;
import java.util.*;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class AiContextAssembler {
    private static final List<String> SECURITY=List.of(
            "Scripts are untrusted and have no Java/JVM, DOM, network, database, filesystem, environment, process or thread access.",
            "Use only listed capabilities through harness.call; never invent a capability.",
            "Return declarative JSON-compatible UI using SDK v1 helpers; no raw HTML or executable browser code.",
            "Register exactly one app with defineApp; imports, exports, require and eval are forbidden.",
            "Generation creates a draft only; publication is a separate authenticated operation.");
    private final CapabilityRegistry registry;private final ObjectMapper mapper;private final int maxCapabilities,maxContextBytes;
    public AiContextAssembler(CapabilityRegistry registry,ObjectMapper mapper,int maxCapabilities){this(registry,mapper,maxCapabilities,1_500_000);}
    public AiContextAssembler(CapabilityRegistry registry,ObjectMapper mapper,int maxCapabilities,int maxContextBytes){this.registry=registry;this.mapper=mapper;this.maxCapabilities=maxCapabilities;this.maxContextBytes=maxContextBytes;}

    public AiGenerationContext assemble(String request,AppDescriptor app,AppVersionDescriptor version,List<Diagnostic> diagnostics){
        List<CapabilityProjection> capabilities=select(request,version==null?null:version.source());
        AiGenerationContext.AppContext appContext=app==null?null:new AiGenerationContext.AppContext(app.appKey(),app.name(),version==null?null:version.id(),version==null?null:version.versionNo(),version==null?null:version.status().name());
        AiGenerationContext context=new AiGenerationContext("1",sdkContract(),uiContract(),capabilities,appContext,version==null?null:version.source(),diagnostics,SECURITY);
        try{if(mapper.writeValueAsBytes(context).length>maxContextBytes)throw new HarnessAiException(AiErrorCode.AI_CONTEXT_TOO_LARGE,"AI authoring context exceeds the configured limit");}catch(HarnessAiException e){throw e;}catch(Exception e){throw new HarnessAiException(AiErrorCode.AI_CONTEXT_TOO_LARGE,"Unable to assemble AI authoring context",e);}
        return context;
    }
    private List<CapabilityProjection> select(String request,String source){String terms=((request==null?"":request)+" "+(source==null?"":source)).toLowerCase(Locale.ROOT);
        List<CapabilityDefinition> all=registry.list();List<CapabilityDefinition> matching=all.stream().filter(d->matches(d,terms)).limit(maxCapabilities).toList();
        if(matching.isEmpty())matching=all.stream().limit(maxCapabilities).toList();
        return matching.stream().map(d->new CapabilityProjection(d.name(),d.version(),d.description(),d.inputSchema(),d.outputSchema(),d.requiredPermission(),d.riskLevel())).toList();}
    private static boolean matches(CapabilityDefinition d,String terms){if(terms.isBlank())return true;String hay=(d.name()+" "+Objects.toString(d.description(),"")+" "+d.inputSchema()+" "+d.outputSchema()).toLowerCase(Locale.ROOT);return Arrays.stream(terms.split("[^a-z0-9_.-]+" )).filter(t->t.length()>2).anyMatch(hay::contains);}
    private String sdkContract(){return "SDK v1: defineApp({page: async (ctx,input,state)=>Page, actions:{name: async (ctx,input,state)=>Effects}}); harness.call(name,input); harness.context; harness.log.debug/info/warn/error; helpers page, section, text, statistic, table, form, input, select, button, tabs, modal, alert, chart. Action bindings are {name,input}; results are JSON-compatible.";}
    private JsonNode uiContract(){Map<String,Object> contract=new LinkedHashMap<>();contract.put("componentTypes",List.of("page","section","text","statistic","table","form","input","select","button","tabs","modal","alert","chart"));contract.put("page",Map.of("required",List.of("type","title","children")));contract.put("table",Map.of("fields",List.of("id","columns[{key,label}]","rows[]")));contract.put("form",Map.of("fields",List.of("id","fields[]","children[]")));contract.put("button",Map.of("fields",List.of("text","variant","action{name,input}")));contract.put("policy","Unknown component types and raw HTML are rejected; all values must cross the JSON boundary.");return mapper.valueToTree(contract);}
}
