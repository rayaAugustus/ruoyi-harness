package com.ruoyi.harness.api;

import java.util.Set;

public record RuntimeIdentity(Long userId, String username, Set<String> permissions) {
    public RuntimeIdentity { permissions = permissions == null ? Set.of() : Set.copyOf(permissions); }
}
