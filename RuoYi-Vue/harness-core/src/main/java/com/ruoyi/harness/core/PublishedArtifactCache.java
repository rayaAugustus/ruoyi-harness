package com.ruoyi.harness.core;

import com.ruoyi.harness.api.ScriptArtifact;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PublishedArtifactCache {
    private final Map<String, ScriptArtifact> artifacts = new ConcurrentHashMap<>();
    private final boolean enabled;
    public PublishedArtifactCache() { this(true); }
    public PublishedArtifactCache(boolean enabled) { this.enabled = enabled; }
    public ScriptArtifact get(String appKey, Long versionId) { return enabled ? artifacts.get(key(appKey, versionId)) : null; }
    public void put(ScriptArtifact artifact) { if (enabled) artifacts.put(key(artifact.appKey(), artifact.id()), artifact); }
    public void invalidateApp(String appKey) { artifacts.keySet().removeIf(k -> k.startsWith(appKey + "@")); }
    private static String key(String appKey, Long versionId) { return appKey + "@" + versionId; }
}
