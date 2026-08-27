package com.ruoyi.harness.ai.service;

import com.ruoyi.harness.ai.context.AiContextAssembler;
import com.ruoyi.harness.ai.model.*;
import com.ruoyi.harness.ai.port.*;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.core.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

public final class AiBuilderService {
    private static final Set<VersionStatus> MUTABLE=Set.of(VersionStatus.DRAFT,VersionStatus.VALIDATED,VersionStatus.REJECTED);
    private static final Pattern NON_KEY=Pattern.compile("[^a-z0-9]+");
    private final boolean enabled;private final String provider;private final String modelName;private final int maxRepairAttempts;private final double temperature;private final int maxOutputTokens,maxContextBytes;
    private final HarnessAiModel model;private final AiContextAssembler contexts;private final AiGenerationValidator generated;
    private final AiSessionRepository sessions;private final AiMessageRepository messages;private final AppRegistryService apps;private final VersionService versions;
    private final PublicationService publication;private final AiPreviewService previews;private final AiAuthorization authorization;private final ObjectMapper mapper;
    public AiBuilderService(boolean enabled,String provider,String modelName,int maxRepairAttempts,double temperature,int maxOutputTokens,int maxContextBytes,HarnessAiModel model,AiContextAssembler contexts,
            AiGenerationValidator generated,AiSessionRepository sessions,AiMessageRepository messages,AppRegistryService apps,VersionService versions,
            PublicationService publication,AiPreviewService previews,AiAuthorization authorization,ObjectMapper mapper){this.enabled=enabled;this.provider=provider;this.modelName=modelName;this.maxRepairAttempts=Math.max(0,Math.min(2,maxRepairAttempts));this.temperature=temperature;this.maxOutputTokens=maxOutputTokens;this.maxContextBytes=maxContextBytes;this.model=model;this.contexts=contexts;this.generated=generated;this.sessions=sessions;this.messages=messages;this.apps=apps;this.versions=versions;this.publication=publication;this.previews=previews;this.authorization=authorization;this.mapper=mapper;}

    public AiStatus status(boolean endpointConfigured,boolean apiKeyConfigured){return new AiStatus(enabled,provider,modelName,endpointConfigured,apiKeyConfigured);}
    @Transactional public SessionView createSession(String appKey,String title,Long userId){
        AppDescriptor app=null;if(!blank(appKey)){authorization.require("harness:app:list");authorization.require("harness:app:edit");app=apps.require(appKey);}AppVersionDescriptor active=app==null?null:selectInitialVersion(app);
        AiSession session=new AiSession();session.setSessionKey(UUID.randomUUID().toString());session.setAppId(app==null?null:app.id());session.setActiveVersionId(active==null?null:active.id());session.setCreatedBy(userId);session.setCreatedAt(Instant.now());session.setUpdatedAt(session.getCreatedAt());session.setTitle(blank(title)?(app==null?"Untitled Harness app":app.name()):title.trim());session.setStatus("ACTIVE");sessions.insert(session);return view(session);
    }
    public List<SessionSummary> list(Long userId){return sessions.findByCreatedBy(userId).stream().map(this::summary).toList();}
    public SessionView get(String key,Long userId){AiSession session=requireOwned(key,userId);if(session.getAppId()!=null)authorization.require("harness:app:list");return view(session);}

    @Transactional public GenerationResponse generate(String key,String prompt,Long userId){
        if(!enabled)throw new HarnessAiException(AiErrorCode.AI_DISABLED,"AI Builder is disabled");if(blank(prompt))throw new HarnessAiException(AiErrorCode.AI_RESPONSE_INVALID,"message is required");
        AiSession session=requireActive(key,userId);AppDescriptor app=app(session);if(app==null)authorization.require("harness:app:create");authorization.require("harness:app:edit");authorization.require("harness:app:validate");storeMessage(session,"USER",prompt,null,null,null,null);
        AppVersionDescriptor current=version(session,app);String provisional=app==null?availableKey(session.getTitle()):app.appKey();
        List<AiMessage> conversation=messages.findBySessionId(session.getId()).stream().filter(m->!"SYSTEM_EVENT".equals(m.getRole())).map(m->new AiMessage(m.getRole(),m.getContent())).toList();
        AiGenerationResult result=null;ValidationResult preflight=null;List<Diagnostic> diagnostics=List.of();
        try {
            for(int attempt=0;attempt<=maxRepairAttempts;attempt++){
                AiGenerationContext context=contexts.assemble(prompt,app,current,diagnostics);
                ensureConversationLimit(conversation);
                result=model.generate(new AiGenerationRequest(conversation,context,new AiGenerationOptions(temperature,maxOutputTokens,true)));
                preflight=generated.validate(provisional,result.script(),result.capabilitiesUsed());
                if(preflight.valid())break;diagnostics=preflight.diagnostics();
                if(attempt<maxRepairAttempts){conversation=new ArrayList<>(conversation);conversation.add(new AiMessage("assistant",structured(result)));conversation.add(new AiMessage("user","Repair the generated Harness script using these validation diagnostics: "+diagnostics));}
            }
            if(app==null){app=apps.create(new AppRegistryService.AppMutation(provisional,session.getTitle(),"Created with Harness AI Builder",session.getTitle(),"magic-stick",0,null),userId);session.setAppId(app.id());}
            AppVersionDescriptor draft=ensureMutableDraft(session,app,result.script(),userId);ValidationResult validation=versions.validate(app.appKey(),draft.id());draft=versions.require(app.appKey(),draft.id());
            session.setActiveVersionId(draft.id());session.setUpdatedAt(Instant.now());sessions.updateLink(session.getId(),app.id(),draft.id(),session.getUpdatedAt());
            storeMessage(session,"ASSISTANT",result.assistantMessage(),result.script(),result.model(),result.provider(),result.usage());
            LinkedHashSet<String> capabilitySet=new LinkedHashSet<>();capabilitySet.addAll(result.capabilitiesUsed());capabilitySet.addAll(generated.inspectCapabilities(result.script()));List<String> used=List.copyOf(capabilitySet);
            return new GenerationResponse(session.getSessionKey(),result.assistantMessage(),app.appKey(),draft.id(),draft.versionNo(),draft.source(),validation,used);
        } catch(RuntimeException e){storeMessage(session,"SYSTEM_EVENT",safeFailure(e),null,null,null,null);throw e;}
    }
    @Transactional public ValidationResult validate(String key,Long userId){authorization.require("harness:app:validate");AiSession session=requireActive(key,userId);AppDescriptor app=requireApp(session);AppVersionDescriptor v=requireActiveVersion(session,app);return versions.validate(app.appKey(),v.id());}
    public RuntimeResponse preview(String key,Long userId,RuntimeIdentity identity,AiPreviewService.AiPreviewRequest request){authorization.require("harness:app:list");AiSession session=requireActive(key,userId);AppDescriptor app=requireApp(session);AppVersionDescriptor v=requireActiveVersion(session,app);return previews.preview(app,v,identity,request);}
    @Transactional public PublishResponse publish(String key,Long userId){authorization.require("harness:app:publish");AiSession session=requireActive(key,userId);AppDescriptor app=requireApp(session);AppVersionDescriptor v=requireActiveVersion(session,app);if(v.status()!=VersionStatus.VALIDATED)throw new HarnessAiException(AiErrorCode.AI_SCRIPT_INVALID,"Only the exact validated draft can be published");publication.publish(app.appKey(),v.id(),userId);storeMessage(session,"SYSTEM_EVENT","Published version "+v.versionNo(),null,null,null,null);return new PublishResponse(app.appKey(),v.id(),v.versionNo());}
    @Transactional public void archive(String key,Long userId){AiSession session=requireOwned(key,userId);session.setStatus("ARCHIVED");session.setUpdatedAt(Instant.now());sessions.archive(session.getId(),session.getUpdatedAt());}

    private AppVersionDescriptor ensureMutableDraft(AiSession session,AppDescriptor app,String source,Long userId){AppVersionDescriptor active=version(session,app);if(active!=null&&MUTABLE.contains(active.status()))return versions.updateSource(app.appKey(),active.id(),source);return versions.create(app.appKey(),"1",source,userId);}
    private AppVersionDescriptor selectInitialVersion(AppDescriptor app){List<AppVersionDescriptor> all=versions.list(app.appKey());return all.stream().filter(v->MUTABLE.contains(v.status())).findFirst().orElseGet(()->app.publishedVersionId()==null?null:all.stream().filter(v->v.id().equals(app.publishedVersionId())).findFirst().orElse(null));}
    private AppDescriptor app(AiSession session){if(session.getAppId()==null)return null;return apps.list().stream().filter(a->a.id().equals(session.getAppId())).findFirst().orElseThrow(()->new HarnessAiException(AiErrorCode.AI_DRAFT_NOT_FOUND,"Builder application no longer exists"));}
    private AppDescriptor requireApp(AiSession session){AppDescriptor app=app(session);if(app==null)throw new HarnessAiException(AiErrorCode.AI_DRAFT_NOT_FOUND,"Session does not have an application draft");return app;}
    private AppVersionDescriptor version(AiSession session,AppDescriptor app){if(app==null||session.getActiveVersionId()==null)return null;return versions.list(app.appKey()).stream().filter(v->v.id().equals(session.getActiveVersionId())).findFirst().orElse(null);}
    private AppVersionDescriptor requireActiveVersion(AiSession session,AppDescriptor app){AppVersionDescriptor v=version(session,app);if(v==null)throw new HarnessAiException(AiErrorCode.AI_DRAFT_NOT_FOUND,"Session does not have an active draft");return v;}
    private AiSession requireOwned(String key,Long userId){AiSession session=sessions.findByKey(key);if(session==null)throw new HarnessAiException(AiErrorCode.AI_SESSION_NOT_FOUND,"Builder session not found");if(!Objects.equals(session.getCreatedBy(),userId))throw new HarnessAiException(AiErrorCode.AI_SESSION_ACCESS_DENIED,"Builder session access denied");return session;}
    private AiSession requireActive(String key,Long userId){AiSession session=requireOwned(key,userId);if(!"ACTIVE".equals(session.getStatus()))throw new HarnessAiException(AiErrorCode.AI_SESSION_ARCHIVED,"Builder session is archived");return session;}
    private SessionView view(AiSession session){AppDescriptor app=app(session);AppVersionDescriptor version=version(session,app);return new SessionView(summary(session),app,version,messages.findBySessionId(session.getId()));}
    private SessionSummary summary(AiSession s){AppDescriptor app=app(s);return new SessionSummary(s.getSessionKey(),s.getTitle(),s.getStatus(),app==null?null:app.appKey(),s.getActiveVersionId(),s.getCreatedAt(),s.getUpdatedAt());}
    private void storeMessage(AiSession session,String role,String content,String source,String model,String provider,Usage usage){AiStoredMessage item=new AiStoredMessage();item.setSessionId(session.getId());item.setRole(role);item.setContent(content);item.setScriptSnapshot(source);item.setModel(model);item.setProvider(provider);if(usage!=null){item.setInputTokens(usage.inputTokens());item.setOutputTokens(usage.outputTokens());}item.setCreatedAt(Instant.now());messages.insert(item);}
    private String availableKey(String title){String base=NON_KEY.matcher(Objects.toString(title,"app").toLowerCase(Locale.ROOT)).replaceAll("-").replaceAll("^-|-$","");if(base.length()<3)base="ai-app";if(base.length()>50)base=base.substring(0,50).replaceAll("-$","");String candidate=base;for(int i=0;i<20;i++){try{apps.require(candidate);candidate=base+"-"+(i+2);}catch(HarnessException e){if(e.getCode()==HarnessErrorCode.APP_NOT_FOUND)return candidate;throw e;}}return base+"-"+UUID.randomUUID().toString().substring(0,8);}
    private String structured(AiGenerationResult result){try{return mapper.writeValueAsString(Map.of("assistantMessage",result.assistantMessage(),"script",result.script(),"capabilitiesUsed",result.capabilitiesUsed()));}catch(Exception e){return result.script();}}
    private static String safeFailure(Throwable error){if(error instanceof HarnessAiException ai)return ai.getCode().name()+": "+ai.getMessage();if(error instanceof HarnessException h)return h.getCode().name()+": "+h.getMessage();return "AI_PROVIDER_UNAVAILABLE: Generation failed";}
    private void ensureConversationLimit(List<AiMessage> conversation){long bytes=conversation.stream().mapToLong(message->message.content().getBytes(java.nio.charset.StandardCharsets.UTF_8).length).sum();if(bytes>maxContextBytes)throw new HarnessAiException(AiErrorCode.AI_CONTEXT_TOO_LARGE,"Builder conversation exceeds the configured context limit");}
    private static boolean blank(String value){return value==null||value.isBlank();}

    public record AiStatus(boolean enabled,String provider,String model,boolean endpointConfigured,boolean apiKeyConfigured){}
    public record SessionSummary(String sessionKey,String title,String status,String appKey,Long activeVersionId,Instant createdAt,Instant updatedAt){}
    public record SessionView(SessionSummary session,AppDescriptor app,AppVersionDescriptor activeVersion,List<AiStoredMessage> messages){}
    public record GenerationResponse(String sessionKey,String assistantMessage,String appKey,Long versionId,Long versionNo,String source,ValidationResult validation,List<String> capabilitiesUsed){}
    public record PublishResponse(String appKey,Long versionId,Long versionNo){}
}
