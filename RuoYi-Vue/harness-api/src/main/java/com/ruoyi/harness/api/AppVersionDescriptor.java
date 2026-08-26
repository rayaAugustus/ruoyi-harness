package com.ruoyi.harness.api;

import java.time.Instant;

public record AppVersionDescriptor(Long id, Long appId, Long versionNo, String sdkVersion,
        String source, String sourceHash, VersionStatus status, Long createdBy,
        Instant createdAt, Instant validatedAt, Instant publishedAt) {}
