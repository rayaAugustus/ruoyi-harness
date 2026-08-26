package com.ruoyi.harness.adapter.config;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.adapter.audit.RuoYiCapabilityAuditSink;
import com.ruoyi.harness.adapter.mapper.*;
import com.ruoyi.harness.adapter.security.RuoYiIdentityAdapter;
import com.ruoyi.harness.capability.*;
import com.ruoyi.harness.core.*;
import com.ruoyi.harness.core.port.*;
import com.ruoyi.harness.runtime.GraalHarnessScriptEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(HarnessProperties.class)
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
public class HarnessConfiguration {
    @Bean public RuntimeLimits harnessRuntimeLimits(HarnessProperties p){var r=p.getRuntime();return new RuntimeLimits(r.getMaxExecutionMillis(),r.getMaxCapabilityCalls(),r.getMaxSourceBytes(),r.getMaxInputBytes(),r.getMaxOutputBytes(),r.getMaxPageNodes(),r.getMaxLogEvents(),r.getMaxTableRowsInDefinition(),r.getMaxJsonDepth());}
    @Bean public CapabilityRegistry capabilityRegistry(){return new CapabilityRegistry();}
    @Bean public JsonSchemaValidator harnessJsonSchemaValidator(){return new JsonSchemaValidator();}
    @Bean public static HarnessCapabilityScanner harnessCapabilityScanner(CapabilityRegistry r,ObjectMapper m){return new HarnessCapabilityScanner(r,m);}
    @Bean public CapabilityAuditSink capabilityAuditSink(HarnessCapabilityLogMapper mapper,HarnessProperties p){return p.getAudit().isEnabled()?new RuoYiCapabilityAuditSink(mapper):CapabilityAuditSink.noop();}
    @Bean public PermissionEvaluator harnessPermissionEvaluator(){return (permission,ctx)->permission==null||permission.isBlank()||com.ruoyi.common.utils.SecurityUtils.hasPermi(ctx.permissions(),permission);}
    @Bean public CapabilityPolicy capabilityPolicy(){return CapabilityPolicy.allowAll();}
    @Bean public CapabilityBridge capabilityBridge(CapabilityRegistry r,JsonSchemaValidator s,PermissionEvaluator p,CapabilityPolicy policy,CapabilityAuditSink audit){return new CapabilityBridge(r,s,p,policy,audit);}
    @Bean public ValidationCapabilityInvoker validationCapabilityInvoker(CapabilityRegistry r,JsonSchemaValidator s,ObjectMapper m){return new ValidationCapabilityInvoker(r,s,m);}
    @Bean public HarnessScriptEngine harnessScriptEngine(ObjectMapper m,RuntimeLimits l,HarnessProperties p){if(!"graaljs".equalsIgnoreCase(p.getRuntime().getEngine()))throw new IllegalArgumentException("Unsupported harness.runtime.engine: "+p.getRuntime().getEngine());return new GraalHarnessScriptEngine(m,l);}
    @Bean public PublishedArtifactCache publishedArtifactCache(HarnessProperties p){return new PublishedArtifactCache(p.getCache().isEnabled());}
    @Bean public RuoYiIdentityAdapter ruoYiIdentityAdapter(){return new RuoYiIdentityAdapter();}
    @Bean public AppAccessEvaluator appAccessEvaluator(){return (permission,identity)->permission==null||permission.isBlank()||com.ruoyi.common.utils.SecurityUtils.hasPermi(identity.permissions(),permission);}
    @Bean public AppRegistryService appRegistryService(HarnessAppMapper m){return new AppRegistryService(m);}
    @Bean public VersionService versionService(AppRegistryService a,HarnessVersionMapper v,HarnessScriptEngine e,ValidationCapabilityInvoker i,ObjectMapper m){return new VersionService(a,v,e,i,m);}
    @Bean public PublicationService publicationService(AppRegistryService a,VersionService v,HarnessAppMapper ar,HarnessVersionMapper vr,PublishedArtifactCache c){return new PublicationService(a,v,ar,vr,c);}
    @Bean public HarnessRuntimeService harnessRuntimeService(AppRegistryService a,VersionService v,HarnessVersionMapper vr,HarnessScriptEngine e,CapabilityBridge b,AppAccessEvaluator access,HarnessExecutionLogMapper logs,PublishedArtifactCache c,ObjectMapper m){return new HarnessRuntimeService(a,v,vr,e,b,access,logs,c,m);}
}
