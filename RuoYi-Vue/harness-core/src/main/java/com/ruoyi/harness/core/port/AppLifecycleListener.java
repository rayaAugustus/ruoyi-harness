package com.ruoyi.harness.core.port;

import com.ruoyi.harness.api.AppDescriptor;

/** Host integration hook invoked inside the application mutation transaction. */
@FunctionalInterface
public interface AppLifecycleListener {
    void onChanged(AppDescriptor app, Long actorId);

    static AppLifecycleListener noop() {
        return (app, actorId) -> { };
    }
}
