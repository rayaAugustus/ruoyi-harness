package com.ruoyi.harness.ai.port;

import com.ruoyi.harness.ai.model.AiSession;
import java.time.Instant;
import java.util.List;

public interface AiSessionRepository {
    AiSession findByKey(String sessionKey); List<AiSession> findByCreatedBy(Long createdBy);
    int insert(AiSession session); int updateLink(Long id, Long appId, Long activeVersionId, Instant updatedAt);
    int archive(Long id, Instant updatedAt);
}
