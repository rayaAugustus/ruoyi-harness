package com.ruoyi.harness.runtime;

import static org.junit.jupiter.api.Assertions.*;
import com.ruoyi.harness.api.*;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GraalHarnessScriptEngineSecurityTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private RuntimeLimits limits=new RuntimeLimits(1500,3,100_000,10_000,10_000,100,3,10,20);
    private GraalHarnessScriptEngine engine=new GraalHarnessScriptEngine(mapper,limits);

    static Stream<String> forbiddenGlobals(){return Stream.of("Polyglot","process","require","fetch","Worker","setTimeout");}
    @ParameterizedTest @MethodSource("forbiddenGlobals")
    void dangerousHostAndPlatformGlobalsAreAbsent(String global){String source="defineApp({page:()=>page({title:'safe',children:[text({value:typeof "+global+"})]})});";
        ScriptExecutionResult result=execute(source,(n,i,c)->mapper.createObjectNode());assertTrue(result.success(),result.diagnostics().toString());assertEquals("undefined",result.value().path("children").get(0).path("value").asText());}

    @Test void javaLangSystemAndRuntimeExecCannotBeReached(){String source="defineApp({page:()=>{Java.type('java.lang.Runtime').getRuntime().exec('echo unsafe');return page({title:'x',children:[]})}});";
        assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}
    @Test void packagesPolyglotEscapeCannotReachHostClass(){String source="defineApp({page:()=>{const x=Packages.java.lang.System.getProperties();return page({title:String(x),children:[]})}});";
        assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}
    @Test void localFileCannotBeRead(){String source="defineApp({page:()=>{require('fs').readFileSync('x');return page({title:'x',children:[]})}});";
        assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}
    @Test void environmentCannotBeRead(){String source="defineApp({page:()=>page({title:String(process.env.PATH),children:[]})});";assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}
    @Test void directNetworkCannotBeOpened(){String source="defineApp({page:async()=>{await fetch('https://example.com');return page({title:'x',children:[]})}});";assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}
    @Test void backgroundThreadCannotBeCreated(){String source="defineApp({page:()=>{new Worker('x');return page({title:'x',children:[]})}});";assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}
    @Test void infiniteLoopTimesOut(){String source="defineApp({page:()=>{while(true){} }});";ScriptExecutionResult result=execute(source,(n,i,c)->mapper.createObjectNode());assertFalse(result.success());assertEquals("SCRIPT_TIMEOUT",result.errorCode());}
    @Test void capabilityQuotaIsEnforced(){String source="defineApp({page:()=>{for(let i=0;i<4;i++)harness.call('x',{});return page({title:'x',children:[]})}});";ScriptExecutionResult result=execute(source,(n,i,c)->mapper.createObjectNode());assertFalse(result.success());assertEquals("CAPABILITY_CALL_LIMIT_EXCEEDED",result.errorCode());}
    @Test void outputLimitIsEnforced(){String source="defineApp({page:()=>page({title:'x',children:[text({value:'x'.repeat(20000)})]})});";ScriptExecutionResult result=execute(source,(n,i,c)->mapper.createObjectNode());assertFalse(result.success());assertEquals("OUTPUT_LIMIT_EXCEEDED",result.errorCode());}
    @Test void unknownCapabilityErrorSurvivesBoundary(){String source="defineApp({page:()=>{harness.call('missing',{});return page({title:'x',children:[]})}});";ScriptExecutionResult result=execute(source,(n,i,c)->{throw new HarnessException(HarnessErrorCode.CAPABILITY_NOT_FOUND,"missing");});assertFalse(result.success());assertEquals("CAPABILITY_NOT_FOUND",result.errorCode());}
    @Test void asyncEntryAndNamedActionWork(){String source="defineApp({page:async()=>page({title:'ok',children:[]}),actions:{go:async(ctx,input)=>({toast:{type:'success',message:input.message}})}});";
        assertTrue(execute(source,(n,i,c)->mapper.createObjectNode()).success());ScriptExecutionResult action=execute(source,"ACTION","go",mapper.valueToTree(java.util.Map.of("message","done")),(n,i,c)->mapper.createObjectNode());assertTrue(action.success());assertEquals("done",action.value().path("toast").path("message").asText());}
    @Test void unknownUiTypeIsRejected(){String source="defineApp({page:()=>page({title:'x',children:[{type:'iframe'}]})});";assertEquals("UI_SCHEMA_INVALID",execute(source,(n,i,c)->mapper.createObjectNode()).errorCode());}
    @Test void functionCannotCrossJsonBoundary(){String source="defineApp({page:()=>({type:'page',title:'x',children:[],bad:()=>1})});";assertFalse(execute(source,(n,i,c)->mapper.createObjectNode()).success());}

    private ScriptExecutionResult execute(String source,CapabilityInvoker invoker){return execute(source,"PAGE","page",mapper.createObjectNode(),invoker);}
    private ScriptExecutionResult execute(String source,String type,String entry,JsonNode input,CapabilityInvoker invoker){ScriptArtifact a=new ScriptArtifact(1L,1L,"test-app",1L,"1",source,null,VersionStatus.VALIDATED);ScriptExecutionContext c=new ScriptExecutionContext(type,entry,input,mapper.createObjectNode(),new RuntimeIdentity(7L,"alice",Set.of()),"en","request","trace",invoker);return engine.execute(a,c);}
}
