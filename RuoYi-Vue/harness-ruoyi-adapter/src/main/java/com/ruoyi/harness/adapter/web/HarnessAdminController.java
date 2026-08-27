package com.ruoyi.harness.adapter.web;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.adapter.mapper.*;
import com.ruoyi.harness.capability.CapabilityRegistry;
import com.ruoyi.harness.core.*;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
@RequestMapping("/harness")
public class HarnessAdminController {
    private final AppRegistryService apps;private final VersionService versions;private final PublicationService publication;
    private final HarnessExecutionLogMapper executionLogs;private final HarnessCapabilityLogMapper capabilityLogs;private final CapabilityRegistry capabilities;
    public HarnessAdminController(AppRegistryService a,VersionService v,PublicationService p,HarnessExecutionLogMapper e,HarnessCapabilityLogMapper c,CapabilityRegistry cr){apps=a;versions=v;publication=p;executionLogs=e;capabilityLogs=c;capabilities=cr;}
    @GetMapping("/apps") @PreAuthorize("@ss.hasPermi('harness:app:list')") public AjaxResult apps(){return AjaxResult.success(apps.list());}
    @PostMapping("/apps") @PreAuthorize("@ss.hasPermi('harness:app:create')") public AjaxResult create(@RequestBody AppRegistryService.AppMutation body){return AjaxResult.success(apps.create(body,SecurityUtils.getUserId()));}
    @GetMapping("/apps/{key}") @PreAuthorize("@ss.hasPermi('harness:app:list')") public AjaxResult app(@PathVariable String key){return AjaxResult.success(apps.require(key));}
    @PutMapping("/apps/{key}") @PreAuthorize("@ss.hasPermi('harness:app:edit')") public AjaxResult update(@PathVariable String key,@RequestBody AppRegistryService.AppMutation body){return AjaxResult.success(apps.update(key,body,SecurityUtils.getUserId()));}
    @PostMapping("/apps/{key}/enable") @PreAuthorize("@ss.hasPermi('harness:app:disable')") public AjaxResult enable(@PathVariable String key){apps.setEnabled(key,true,SecurityUtils.getUserId());return AjaxResult.success();}
    @PostMapping("/apps/{key}/disable") @PreAuthorize("@ss.hasPermi('harness:app:disable')") public AjaxResult disable(@PathVariable String key){apps.setEnabled(key,false,SecurityUtils.getUserId());return AjaxResult.success();}
    @GetMapping("/apps/{key}/versions") @PreAuthorize("@ss.hasPermi('harness:app:list')") public AjaxResult versions(@PathVariable String key){return AjaxResult.success(versions.list(key));}
    @PostMapping("/apps/{key}/versions") @PreAuthorize("@ss.hasPermi('harness:app:edit')") public AjaxResult createVersion(@PathVariable String key,@RequestBody VersionMutation b){return AjaxResult.success(versions.create(key,b.sdkVersion(),b.source(),SecurityUtils.getUserId()));}
    @GetMapping("/apps/{key}/versions/{id}") @PreAuthorize("@ss.hasPermi('harness:app:list')") public AjaxResult version(@PathVariable String key,@PathVariable Long id){return AjaxResult.success(versions.require(key,id));}
    @PutMapping("/apps/{key}/versions/{id}/source") @PreAuthorize("@ss.hasPermi('harness:app:edit')") public AjaxResult source(@PathVariable String key,@PathVariable Long id,@RequestBody VersionMutation b){return AjaxResult.success(versions.updateSource(key,id,b.source()));}
    @PostMapping("/apps/{key}/versions/{id}/validate") @PreAuthorize("@ss.hasPermi('harness:app:validate')") public AjaxResult validate(@PathVariable String key,@PathVariable Long id){return AjaxResult.success(versions.validate(key,id));}
    @PostMapping("/apps/{key}/versions/{id}/publish") @PreAuthorize("@ss.hasPermi('harness:app:publish')") public AjaxResult publish(@PathVariable String key,@PathVariable Long id){publication.publish(key,id,SecurityUtils.getUserId());return AjaxResult.success();}
    @PostMapping("/apps/{key}/rollback/{id}") @PreAuthorize("@ss.hasPermi('harness:app:rollback')") public AjaxResult rollback(@PathVariable String key,@PathVariable Long id){publication.rollback(key,id,SecurityUtils.getUserId());return AjaxResult.success();}
    @DeleteMapping("/apps/{key}/versions/{id}") @PreAuthorize("@ss.hasPermi('harness:app:edit')") public AjaxResult delete(@PathVariable String key,@PathVariable Long id){versions.deleteDraft(key,id);return AjaxResult.success();}
    @GetMapping("/audit/executions") @PreAuthorize("@ss.hasPermi('harness:audit:view')") public AjaxResult executions(@RequestParam Map<String,Object> filters){return AjaxResult.success(executionLogs.search(filters));}
    @GetMapping("/audit/capabilities") @PreAuthorize("@ss.hasPermi('harness:audit:view')") public AjaxResult capabilityLogs(@RequestParam Map<String,Object> filters){return AjaxResult.success(capabilityLogs.search(filters));}
    @GetMapping("/capabilities") @PreAuthorize("@ss.hasPermi('harness:app:list') or @ss.hasPermi('harness:ai:use')") public AjaxResult capabilities(){return AjaxResult.success(capabilities.list().stream().map(d->new CapabilityView(d.name(),d.version(),d.description(),d.inputSchema(),d.outputSchema(),d.requiredPermission(),d.riskLevel())).toList());}
    public record VersionMutation(String sdkVersion,String source){}
    public record CapabilityView(String name,String version,String description,tools.jackson.databind.JsonNode inputSchema,tools.jackson.databind.JsonNode outputSchema,String requiredPermission,RiskLevel riskLevel){}
}
