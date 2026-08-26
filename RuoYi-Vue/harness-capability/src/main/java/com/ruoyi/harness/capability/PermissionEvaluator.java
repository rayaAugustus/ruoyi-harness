package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.CapabilityContext;

@FunctionalInterface
public interface PermissionEvaluator { boolean isAllowed(String permission, CapabilityContext context); }
