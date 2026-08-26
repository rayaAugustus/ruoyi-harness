package com.ruoyi.harness.api;

public record ScriptArtifact(Long id, Long appId, String appKey, Long versionNo,
        String sdkVersion, String source, String sourceHash, VersionStatus status) {}
