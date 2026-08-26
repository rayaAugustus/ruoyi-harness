package com.ruoyi.harness.adapter.web;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.adapter.security.RuoYiIdentityAdapter;
import com.ruoyi.harness.core.HarnessRuntimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
@RequestMapping("/harness/runtime/apps/{appKey}")
public class HarnessRuntimeController {
    private final HarnessRuntimeService runtime;private final RuoYiIdentityAdapter identities;
    public HarnessRuntimeController(HarnessRuntimeService r,RuoYiIdentityAdapter i){runtime=r;identities=i;}
    @GetMapping public HarnessRuntimeService.RuntimeDescriptor resolve(@PathVariable String appKey){return runtime.resolve(appKey,identities.current());}
    @PostMapping("/render") public RuntimeResponse render(@PathVariable String appKey,@RequestBody(required=false) RenderRequest body,HttpServletRequest request){return runtime.render(appKey,body,identities.current(),request.getHeader("X-Request-ID"),request.getLocale().toLanguageTag());}
    @PostMapping("/actions/{actionName}") public RuntimeResponse action(@PathVariable String appKey,@PathVariable String actionName,@RequestBody ActionRequest body,HttpServletRequest request){return runtime.action(appKey,actionName,body,identities.current(),request.getHeader("X-Request-ID"),request.getLocale().toLanguageTag());}
}
