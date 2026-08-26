package com.ruoyi.harness.capability;

import com.ruoyi.harness.api.CapabilityDefinition;
import com.ruoyi.harness.api.HarnessErrorCode;
import com.ruoyi.harness.api.HarnessException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CapabilityRegistry {
    private final Map<String, CapabilityDefinition> definitions = new ConcurrentHashMap<>();

    public void register(CapabilityDefinition definition) {
        String key = key(definition.name(), definition.version());
        if (definitions.putIfAbsent(key, definition) != null) {
            throw new HarnessException(HarnessErrorCode.CAPABILITY_CONFLICT,
                    "Capability already registered: " + key);
        }
    }

    public CapabilityDefinition require(String name, String version) {
        CapabilityDefinition result = definitions.get(key(name, version));
        if (result == null) throw new HarnessException(HarnessErrorCode.CAPABILITY_NOT_FOUND,
                "Unknown capability: " + name + "@" + version);
        return result;
    }

    public CapabilityDefinition requireLatest(String name) {
        return definitions.values().stream().filter(d -> d.name().equals(name))
                .max(Comparator.comparing(CapabilityDefinition::version))
                .orElseThrow(() -> new HarnessException(HarnessErrorCode.CAPABILITY_NOT_FOUND,
                        "Unknown capability: " + name));
    }

    public List<CapabilityDefinition> list() {
        return definitions.values().stream().sorted(Comparator.comparing(CapabilityDefinition::name)
                .thenComparing(CapabilityDefinition::version)).toList();
    }

    private static String key(String name, String version) { return name + "@" + version; }
}
