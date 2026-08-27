package com.ruoyi.harness.adapter.web;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.harness.adapter.config.HarnessAiProperties;
import com.ruoyi.harness.adapter.security.RuoYiIdentityAdapter;
import com.ruoyi.harness.ai.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
@RequestMapping("/harness/ai")
public class HarnessAiBuilderController {
    private final AiBuilderService builder;private final HarnessAiProperties properties;private final RuoYiIdentityAdapter identities;
    public HarnessAiBuilderController(AiBuilderService builder,HarnessAiProperties properties,RuoYiIdentityAdapter identities){this.builder=builder;this.properties=properties;this.identities=identities;}
    @GetMapping("/status") @PreAuthorize("@ss.hasPermi('harness:ai:use')") public AjaxResult status(){return AjaxResult.success(builder.status(configured(properties.getBaseUrl()),configured(properties.getApiKey())));}
    @GetMapping("/sessions") @PreAuthorize("@ss.hasPermi('harness:ai:session:list')") public AjaxResult sessions(){return AjaxResult.success(builder.list(SecurityUtils.getUserId()));}
    @PostMapping("/sessions") @PreAuthorize("@ss.hasPermi('harness:ai:use')") public AjaxResult create(@RequestBody(required=false) CreateSession body){return AjaxResult.success(builder.createSession(body==null?null:body.appKey(),body==null?null:body.title(),SecurityUtils.getUserId()));}
    @GetMapping("/sessions/{key}") @PreAuthorize("@ss.hasPermi('harness:ai:session:view')") public AjaxResult session(@PathVariable String key){return AjaxResult.success(builder.get(key,SecurityUtils.getUserId()));}
    @PostMapping("/sessions/{key}/messages") @PreAuthorize("@ss.hasPermi('harness:ai:use')") public AjaxResult message(@PathVariable String key,@RequestBody UserMessage body){return AjaxResult.success(builder.generate(key,body.message(),SecurityUtils.getUserId()));}
    @PostMapping("/sessions/{key}/preview") @PreAuthorize("@ss.hasPermi('harness:ai:use')") public AjaxResult preview(@PathVariable String key,@RequestBody(required=false) AiPreviewService.AiPreviewRequest body){return AjaxResult.success(builder.preview(key,SecurityUtils.getUserId(),identities.current(),body));}
    @PostMapping("/sessions/{key}/validate") @PreAuthorize("@ss.hasPermi('harness:ai:use') and @ss.hasPermi('harness:app:validate')") public AjaxResult validate(@PathVariable String key){return AjaxResult.success(builder.validate(key,SecurityUtils.getUserId()));}
    @PostMapping("/sessions/{key}/publish") @PreAuthorize("@ss.hasPermi('harness:ai:use') and @ss.hasPermi('harness:app:publish')") public AjaxResult publish(@PathVariable String key){return AjaxResult.success(builder.publish(key,SecurityUtils.getUserId()));}
    @PostMapping("/sessions/{key}/archive") @PreAuthorize("@ss.hasPermi('harness:ai:use')") public AjaxResult archive(@PathVariable String key){builder.archive(key,SecurityUtils.getUserId());return AjaxResult.success();}
    private static boolean configured(String value){return value!=null&&!value.isBlank();}
    public record CreateSession(String appKey,String title){}
    public record UserMessage(String message){}
}
