package com.ruoyi.harness.adapter.mapper;

import com.ruoyi.harness.ai.port.AiSessionRepository;
import java.time.Instant;
import org.apache.ibatis.annotations.Param;

public interface HarnessAiSessionMapper extends AiSessionRepository {
    @Override int updateLink(@Param("id")Long id,@Param("appId")Long appId,@Param("activeVersionId")Long activeVersionId,@Param("updatedAt")Instant updatedAt);
    @Override int archive(@Param("id")Long id,@Param("updatedAt")Instant updatedAt);
}
