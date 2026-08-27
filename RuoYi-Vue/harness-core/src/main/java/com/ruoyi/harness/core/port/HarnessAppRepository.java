package com.ruoyi.harness.core.port;

import com.ruoyi.harness.core.domain.HarnessApp;
import java.util.List;

public interface HarnessAppRepository {
    HarnessApp findByKey(String appKey); HarnessApp findByKeyForUpdate(String appKey); HarnessApp findById(Long id); List<HarnessApp> findAll();
    int insert(HarnessApp app); int update(HarnessApp app); int setEnabled(Long id, boolean enabled, Long actorId);
    int setPublishedVersion(Long id, Long versionId, Long actorId);
}
