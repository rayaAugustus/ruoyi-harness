package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.RiskLevel;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HarnessCapability {
    String name(); String version() default "1"; String description() default "";
    String permission() default ""; RiskLevel risk() default RiskLevel.READ;
    String inputSchema() default "{\"type\":\"object\"}";
    String outputSchema() default "{}";
}
