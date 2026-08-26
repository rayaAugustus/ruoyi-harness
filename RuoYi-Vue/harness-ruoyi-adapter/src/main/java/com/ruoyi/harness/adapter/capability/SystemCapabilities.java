package com.ruoyi.harness.adapter.capability;

import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.harness.api.*;
import com.ruoyi.harness.capability.CapabilityRegistry;
import com.ruoyi.system.service.ISysDictTypeService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
public class SystemCapabilities implements InitializingBean {
    private final CapabilityRegistry registry;private final ObjectMapper mapper;private final ISysDictTypeService dictionaries;
    public SystemCapabilities(CapabilityRegistry r,ObjectMapper m,ISysDictTypeService d){registry=r;mapper=m;dictionaries=d;}
    @Override public void afterPropertiesSet() throws Exception {
        register("system.user.current","Current authenticated user","",RiskLevel.READ,"{\"type\":\"object\",\"additionalProperties\":false}","{\"type\":\"object\"}",(in,ctx)->mapper.valueToTree(Map.of("id",ctx.userId(),"name",ctx.username())));
        register("system.time.now","Current host time","",RiskLevel.READ,"{\"type\":\"object\",\"additionalProperties\":false}","{\"type\":\"object\"}",(in,ctx)->mapper.valueToTree(Map.of("instant",Instant.now().toString())));
        register("system.dict.options","RuoYi dictionary options","system:dict:list",RiskLevel.READ,
            "{\"type\":\"object\",\"required\":[\"dictType\"],\"additionalProperties\":false,\"properties\":{\"dictType\":{\"type\":\"string\",\"maxLength\":100}}}","{\"type\":\"array\"}",(in,ctx)->{
                List<SysDictData> rows=dictionaries.selectDictDataByType(in.path("dictType").asText());
                return mapper.valueToTree(rows.stream().map(d->Map.of("label",d.getDictLabel(),"value",d.getDictValue())).toList());});
    }
    private void register(String name,String description,String permission,RiskLevel risk,String input,String output,CapabilityHandler handler)throws Exception{
        registry.register(new CapabilityDefinition(name,"1",description,mapper.readTree(input),mapper.readTree(output),permission,risk,handler));}
}
