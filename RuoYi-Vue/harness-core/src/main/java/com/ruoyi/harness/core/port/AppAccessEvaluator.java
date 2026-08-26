package com.ruoyi.harness.core.port;

import com.ruoyi.harness.api.RuntimeIdentity;

@FunctionalInterface
public interface AppAccessEvaluator { boolean isAllowed(String permission, RuntimeIdentity identity); }
