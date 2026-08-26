package com.ruoyi.harness.core.port;

import com.ruoyi.harness.api.VersionStatus;
import com.ruoyi.harness.core.domain.HarnessAppVersion;
import java.time.Instant;
import java.util.List;

public interface HarnessVersionRepository {
    HarnessAppVersion findById(Long id); List<HarnessAppVersion> findByAppId(Long appId); Long nextVersionNo(Long appId);
    int insert(HarnessAppVersion version); int updateDraftSource(Long id, String source, VersionStatus status);
    int updateValidation(Long id, String sourceHash, VersionStatus status, Instant validatedAt);
    int updateStatus(Long id, VersionStatus status, Instant publishedAt); int deleteDraft(Long id);
}
