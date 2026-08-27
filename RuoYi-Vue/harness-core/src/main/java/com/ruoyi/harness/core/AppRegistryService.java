package com.ruoyi.harness.core;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.core.domain.HarnessApp;
import com.ruoyi.harness.core.port.AppLifecycleListener;
import com.ruoyi.harness.core.port.HarnessAppRepository;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;

public class AppRegistryService {
    private static final Pattern KEY = Pattern.compile("^[a-z][a-z0-9-]{1,62}[a-z0-9]$");
    private final HarnessAppRepository apps;
    private final AppLifecycleListener lifecycle;
    public AppRegistryService(HarnessAppRepository apps) { this(apps, AppLifecycleListener.noop()); }
    public AppRegistryService(HarnessAppRepository apps, AppLifecycleListener lifecycle) { this.apps = apps; this.lifecycle = lifecycle; }

    public List<AppDescriptor> list() { return apps.findAll().stream().map(AppRegistryService::descriptor).toList(); }
    public AppDescriptor require(String appKey) { return descriptor(requireEntity(appKey)); }
    public HarnessApp requireEntity(String appKey) {
        HarnessApp app = apps.findByKey(appKey);
        if (app == null) throw new HarnessException(HarnessErrorCode.APP_NOT_FOUND, "Application not found");
        return app;
    }
    HarnessApp requireLockedEntity(String appKey) {
        HarnessApp app = apps.findByKeyForUpdate(appKey);
        if (app == null) throw new HarnessException(HarnessErrorCode.APP_NOT_FOUND, "Application not found");
        return app;
    }

    @Transactional
    public AppDescriptor create(AppMutation request, Long actorId) {
        validate(request, true);
        if (apps.findByKey(request.appKey()) != null) throw new HarnessException(HarnessErrorCode.APP_KEY_CONFLICT, "Application key already exists");
        HarnessApp app = new HarnessApp(); app.setAppKey(request.appKey()); apply(app, request);
        app.setRequiredPermission(blank(request.requiredPermission()) ? "harness:app:" + request.appKey() + ":access" : request.requiredPermission());
        app.setEnabled(Boolean.TRUE); app.setCreatedBy(actorId); app.setUpdatedBy(actorId); app.setCreatedAt(Instant.now()); app.setUpdatedAt(Instant.now());
        apps.insert(app); AppDescriptor descriptor = descriptor(app); lifecycle.onChanged(descriptor, actorId); return descriptor;
    }

    @Transactional
    public AppDescriptor update(String appKey, AppMutation request, Long actorId) {
        HarnessApp app = requireEntity(appKey); validate(request, false); apply(app, request);
        app.setUpdatedBy(actorId); app.setUpdatedAt(Instant.now()); apps.update(app); AppDescriptor descriptor = descriptor(app); lifecycle.onChanged(descriptor, actorId); return descriptor;
    }

    @Transactional public void setEnabled(String appKey, boolean enabled, Long actorId) { HarnessApp app=requireEntity(appKey); apps.setEnabled(app.getId(), enabled, actorId); app.setEnabled(enabled); app.setUpdatedBy(actorId); app.setUpdatedAt(Instant.now()); lifecycle.onChanged(descriptor(app), actorId); }

    private static void apply(HarnessApp app, AppMutation request) {
        app.setName(request.name()); app.setDescription(request.description()); app.setRouteTitle(request.routeTitle());
        app.setIcon(request.icon()); app.setOrderNum(request.orderNum() == null ? 0 : request.orderNum());
        if (!blank(request.requiredPermission())) app.setRequiredPermission(request.requiredPermission());
    }
    private static void validate(AppMutation request, boolean creating) {
        if (creating && (request.appKey() == null || !KEY.matcher(request.appKey()).matches()))
            throw new HarnessException(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "appKey must be lowercase kebab-case");
        if (blank(request.name()) || blank(request.routeTitle())) throw new HarnessException(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "name and routeTitle are required");
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    static AppDescriptor descriptor(HarnessApp a) { return new AppDescriptor(a.getId(), a.getAppKey(), a.getName(), a.getDescription(), a.getRouteTitle(), a.getIcon(), a.getOrderNum(), a.getRequiredPermission(), Boolean.TRUE.equals(a.getEnabled()), a.getPublishedVersionId()); }

    public record AppMutation(String appKey, String name, String description, String routeTitle, String icon, Integer orderNum, String requiredPermission) {}
}
