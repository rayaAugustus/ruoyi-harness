package com.ruoyi.harness.ai;

import static org.junit.jupiter.api.Assertions.*;
import com.ruoyi.harness.ai.context.AiContextAssembler;
import com.ruoyi.harness.ai.model.*;
import com.ruoyi.harness.ai.provider.*;
import com.ruoyi.harness.ai.service.*;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.capability.*;
import com.ruoyi.harness.runtime.GraalHarnessScriptEngine;
import java.time.Duration;
import java.util.*;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class AiBuilderBoundaryTest {
    private final ObjectMapper mapper=new ObjectMapper();

    @Test void parsesOnlyStrictStructuredOutput(){StructuredAiResponseParser parser=new StructuredAiResponseParser(mapper);
        AiGenerationResult result=parser.parse("{\"assistantMessage\":\"Done\",\"script\":\"defineApp({});\",\"capabilitiesUsed\":[\"system.time.now\"]}","m","p",Usage.empty());
        assertEquals("defineApp({});",result.script());assertEquals(List.of("system.time.now"),result.capabilitiesUsed());
        HarnessAiException malformed=assertThrows(HarnessAiException.class,()->parser.parse("```js defineApp({}) ```","m","p",Usage.empty()));assertEquals(AiErrorCode.AI_RESPONSE_INVALID,malformed.getCode());}

    @Test void contextContainsSdkAndSafeCapabilityProjectionOnly() throws Exception {CapabilityRegistry registry=new CapabilityRegistry();registry.register(capability("example.customer.list",RiskLevel.READ));
        AiGenerationContext context=new AiContextAssembler(registry,mapper,50).assemble("customer list",null,null,List.of());String json=mapper.writeValueAsString(context);
        assertTrue(context.sdkContract().contains("defineApp"));assertTrue(context.uiContract().toString().contains("table"));assertEquals("example.customer.list",context.capabilities().get(0).name());assertFalse(json.contains("handler"));}
    @Test void oversizedContextHasStableFailureCode(){HarnessAiException error=assertThrows(HarnessAiException.class,()->new AiContextAssembler(new CapabilityRegistry(),mapper,10,32).assemble("build",null,null,List.of()));assertEquals(AiErrorCode.AI_CONTEXT_TOO_LARGE,error.getCode());}

    @Test void rejectsInventedCapabilitiesBeforeExecution(){CapabilityRegistry registry=new CapabilityRegistry();HarnessScriptEngine engine=new GraalHarnessScriptEngine(mapper,RuntimeLimits.defaults());AiGenerationValidator validator=new AiGenerationValidator(engine,registry,new ValidationCapabilityInvoker(registry,new JsonSchemaValidator(),mapper),mapper,524288);
        HarnessAiException error=assertThrows(HarnessAiException.class,()->validator.validate("test-app","defineApp({page:()=>page({title:'x',children:[]})});",List.of("invented.write")));
        assertEquals(AiErrorCode.AI_CAPABILITY_UNKNOWN,error.getCode());}

    @Test void previewAllowsReadAndDeniesEveryWriteRisk(){assertDoesNotThrow(()->preview(RiskLevel.READ));for(RiskLevel risk:List.of(RiskLevel.WRITE,RiskLevel.SENSITIVE_WRITE,RiskLevel.ADMIN)){
        HarnessException error=assertThrows(HarnessException.class,()->preview(risk));assertEquals(HarnessErrorCode.CAPABILITY_POLICY_DENIED,error.getCode());}}
    @Test void previewReadStillRequiresNormalUserPermission(){HarnessException error=assertThrows(HarnessException.class,()->preview(RiskLevel.READ,false));assertEquals(HarnessErrorCode.CAPABILITY_PERMISSION_DENIED,error.getCode());}

    @Test void disabledProviderDoesNotMakeNetworkRequest(){var provider=new OpenAiCompatibleHarnessAiModel(mapper,false,"http://127.0.0.1","secret","model",Duration.ofSeconds(1),Duration.ofSeconds(1),"prompt");
        HarnessAiException error=assertThrows(HarnessAiException.class,()->provider.generate(new AiGenerationRequest(List.of(),null,new AiGenerationOptions(.2,10,true))));assertEquals(AiErrorCode.AI_DISABLED,error.getCode());}

    @Test void providerFailuresMapToStableCodes() throws Exception {assertProviderCode(401,0,"{}",AiErrorCode.AI_PROVIDER_AUTH_FAILED);assertProviderCode(429,0,"{}",AiErrorCode.AI_RATE_LIMITED);assertProviderCode(200,0,"{}",AiErrorCode.AI_RESPONSE_INVALID);assertProviderCode(200,200,"{}",AiErrorCode.AI_TIMEOUT);}

    private RuntimeResponse preview(RiskLevel risk){return preview(risk,true);}private RuntimeResponse preview(RiskLevel risk,boolean permitted){CapabilityRegistry registry=new CapabilityRegistry();registry.register(capability("test.operation",risk));String script="defineApp({page:async()=>page({title:(await harness.call('test.operation',{})).value,children:[]})});";
        AiPreviewService service=new AiPreviewService(new GraalHarnessScriptEngine(mapper,RuntimeLimits.defaults()),registry,new JsonSchemaValidator(),(p,c)->permitted,CapabilityAuditSink.noop(),mapper);
        AppDescriptor app=new AppDescriptor(1L,"test-app","Test","","Test","x",0,"access",true,null);AppVersionDescriptor version=new AppVersionDescriptor(2L,1L,1L,"1",script,"sha256:test",VersionStatus.VALIDATED,1L,null,null,null);
        return service.preview(app,version,new RuntimeIdentity(1L,"user",Set.of("operation")),new AiPreviewService.AiPreviewRequest(2L,null,null,null,null));}
    private CapabilityDefinition capability(String name,RiskLevel risk){return new CapabilityDefinition(name,"1","Safe projection",mapper.createObjectNode(),mapper.createObjectNode(),"operation",risk,(input,ctx)->mapper.valueToTree(Map.of("value","ok")));}
    private void assertProviderCode(int status,long delay,String body,AiErrorCode expected) throws Exception {HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/chat/completions",exchange->{try{if(delay>0)Thread.sleep(delay);byte[] data=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(status,data.length);exchange.getResponseBody().write(data);}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{exchange.close();}});server.start();try{var provider=new OpenAiCompatibleHarnessAiModel(mapper,true,"http://127.0.0.1:"+server.getAddress().getPort(),"secret","model",Duration.ofSeconds(1),Duration.ofMillis(50),"prompt");HarnessAiException error=assertThrows(HarnessAiException.class,()->provider.generate(new AiGenerationRequest(List.of(new AiMessage("user","build")),null,new AiGenerationOptions(.2,10,true))));assertEquals(expected,error.getCode());}finally{server.stop(0);}}
}
