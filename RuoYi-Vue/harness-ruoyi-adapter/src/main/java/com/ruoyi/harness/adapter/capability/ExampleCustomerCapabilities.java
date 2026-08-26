package com.ruoyi.harness.adapter.capability;

import com.ruoyi.harness.api.CapabilityContext;
import com.ruoyi.harness.api.RiskLevel;
import com.ruoyi.harness.capability.HarnessCapability;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Example domain proving that ordinary Spring services use the same public capability API. */
@Component
@ConditionalOnProperty(prefix="harness",name="enabled",havingValue="true",matchIfMissing=true)
public class ExampleCustomerCapabilities {
    private final ObjectMapper mapper; private final Map<Long,Customer> customers=new ConcurrentHashMap<>();private final AtomicLong ids=new AtomicLong(2);
    public ExampleCustomerCapabilities(ObjectMapper mapper){this.mapper=mapper;customers.put(1L,new Customer(1L,"Acme","A"));customers.put(2L,new Customer(2L,"Globex","B"));}
    @HarnessCapability(name="example.customer.list",description="List example customers",permission="example:customer:list",risk=RiskLevel.READ,
      inputSchema="{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"page\":{\"type\":\"integer\"},\"size\":{\"type\":\"integer\"}}}",
      outputSchema="{\"type\":\"object\",\"required\":[\"items\",\"total\"],\"properties\":{\"items\":{\"type\":\"array\"},\"total\":{\"type\":\"integer\"}}}")
    public JsonNode list(JsonNode input,CapabilityContext context){int page=Math.max(1,input.path("page").asInt(1));int size=Math.min(100,Math.max(1,input.path("size").asInt(20)));List<Customer> all=customers.values().stream().sorted(Comparator.comparing(Customer::id)).toList();int from=Math.min(all.size(),(page-1)*size),to=Math.min(all.size(),from+size);return mapper.valueToTree(Map.of("items",all.subList(from,to),"total",all.size()));}
    @HarnessCapability(name="example.customer.get",permission="example:customer:list",risk=RiskLevel.READ,inputSchema="{\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"integer\"}}}")
    public Customer get(CustomerId input,CapabilityContext context){return customers.get(input.id());}
    @HarnessCapability(name="example.customer.create",permission="example:customer:add",risk=RiskLevel.WRITE,inputSchema="{\"type\":\"object\",\"required\":[\"name\",\"level\"],\"additionalProperties\":false,\"properties\":{\"name\":{\"type\":\"string\",\"maxLength\":100},\"level\":{\"type\":\"string\",\"enum\":[\"A\",\"B\",\"C\"]}}}")
    public Customer create(CustomerMutation input,CapabilityContext context){long id=ids.incrementAndGet();Customer c=new Customer(id,input.name(),input.level());customers.put(id,c);return c;}
    @HarnessCapability(name="example.customer.update",permission="example:customer:edit",risk=RiskLevel.WRITE,inputSchema="{\"type\":\"object\",\"required\":[\"id\",\"name\",\"level\"],\"additionalProperties\":false,\"properties\":{\"id\":{\"type\":\"integer\"},\"name\":{\"type\":\"string\",\"maxLength\":100},\"level\":{\"type\":\"string\",\"enum\":[\"A\",\"B\",\"C\"]}}}")
    public Customer update(CustomerUpdate input,CapabilityContext context){Customer c=new Customer(input.id(),input.name(),input.level());customers.put(input.id(),c);return c;}
    public record Customer(Long id,String name,String level){} public record CustomerId(Long id){} public record CustomerMutation(String name,String level){} public record CustomerUpdate(Long id,String name,String level){}
}
