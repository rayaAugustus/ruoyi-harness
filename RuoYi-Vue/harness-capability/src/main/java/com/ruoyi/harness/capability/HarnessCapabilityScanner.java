package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.CapabilityContext;
import com.ruoyi.harness.api.CapabilityDefinition;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class HarnessCapabilityScanner implements BeanPostProcessor {
    private final CapabilityRegistry registry;
    private final ObjectMapper mapper;

    public HarnessCapabilityScanner(CapabilityRegistry registry, ObjectMapper mapper) {
        this.registry = registry; this.mapper = mapper;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> type = AopUtils.getTargetClass(bean);
        Arrays.stream(type.getMethods()).filter(m -> m.isAnnotationPresent(HarnessCapability.class))
                .forEach(m -> register(bean, m, m.getAnnotation(HarnessCapability.class)));
        return bean;
    }

    private void register(Object bean, Method method, HarnessCapability annotation) {
        JsonNode inputSchema = read(annotation.inputSchema());
        JsonNode outputSchema = read(annotation.outputSchema());
        registry.register(new CapabilityDefinition(annotation.name(), annotation.version(), annotation.description(),
                inputSchema, outputSchema, annotation.permission(), annotation.risk(), (input, context) -> {
                    Object[] args = Arrays.stream(method.getParameterTypes()).map(parameter -> {
                        if (parameter == CapabilityContext.class) return context;
                        if (parameter == JsonNode.class) return input;
                        return mapper.treeToValue(input, parameter);
                    }).toArray();
                    Object result = method.invoke(bean, args);
                    return mapper.valueToTree(result);
                }));
    }

    private JsonNode read(String json) {
        try { return mapper.readTree(json); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid capability schema", e); }
    }
}
