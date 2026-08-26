package com.ruoyi.harness.api;

public record AppDescriptor(Long id, String appKey, String name, String description,
        String routeTitle, String icon, Integer orderNum, String requiredPermission,
        boolean enabled, Long publishedVersionId) {}
