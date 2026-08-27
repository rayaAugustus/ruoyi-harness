package com.ruoyi.harness.adapter.config;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.harness.ai.context.AiContextAssembler;
import com.ruoyi.harness.ai.port.*;
import com.ruoyi.harness.ai.provider.OpenAiCompatibleHarnessAiModel;
import com.ruoyi.harness.ai.service.*;
import com.ruoyi.harness.api.HarnessScriptEngine;
import com.ruoyi.harness.adapter.mapper.*;
import com.ruoyi.harness.capability.*;
import com.ruoyi.harness.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(HarnessAiProperties.class)
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
public class HarnessAiConfiguration {
    @Bean public HarnessAiModel harnessAiModel(ObjectMapper mapper,HarnessAiProperties p){return new OpenAiCompatibleHarnessAiModel(mapper,p.isEnabled(),p.getBaseUrl(),p.getApiKey(),p.getModel(),p.getConnectTimeout(),p.getReadTimeout(),prompt());}
    @Bean public AiContextAssembler aiContextAssembler(CapabilityRegistry registry,ObjectMapper mapper,HarnessAiProperties p){return new AiContextAssembler(registry,mapper,p.getMaxContextCapabilities(),p.getMaxContextBytes());}
    @Bean public AiGenerationValidator aiGenerationValidator(HarnessScriptEngine engine,CapabilityRegistry registry,ValidationCapabilityInvoker validation,ObjectMapper mapper,HarnessProperties p){return new AiGenerationValidator(engine,registry,validation,mapper,p.getRuntime().getMaxSourceBytes());}
    @Bean public AiPreviewService aiPreviewService(HarnessScriptEngine engine,CapabilityRegistry registry,JsonSchemaValidator schemas,PermissionEvaluator permissions,CapabilityAuditSink audit,ObjectMapper mapper){return new AiPreviewService(engine,registry,schemas,permissions,audit,mapper);}
    @Bean public AiAuthorization aiAuthorization(){return permission->{if(!SecurityUtils.hasPermi(SecurityUtils.getLoginUser().getPermissions(),permission))throw new org.springframework.security.access.AccessDeniedException("Permission denied");};}
    @Bean public AiBuilderService aiBuilderService(HarnessAiProperties p,HarnessAiModel model,AiContextAssembler contexts,AiGenerationValidator validator,HarnessAiSessionMapper sessions,HarnessAiMessageMapper messages,AppRegistryService apps,VersionService versions,PublicationService publication,AiPreviewService previews,AiAuthorization auth,ObjectMapper mapper){return new AiBuilderService(p.isEnabled(),p.getProvider(),p.getModel(),p.getMaxRepairAttempts(),p.getTemperature(),p.getMaxOutputTokens(),p.getMaxContextBytes(),model,contexts,validator,sessions,messages,apps,versions,publication,previews,auth,mapper);}
    private static String prompt(){try{return new ClassPathResource("prompts/builder-system-v1.md").getContentAsString(StandardCharsets.UTF_8);}catch(IOException e){throw new IllegalStateException("Missing Harness AI system prompt",e);}}
}
