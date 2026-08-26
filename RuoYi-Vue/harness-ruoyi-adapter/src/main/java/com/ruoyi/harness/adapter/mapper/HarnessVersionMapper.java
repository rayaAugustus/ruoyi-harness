package com.ruoyi.harness.adapter.mapper;

import com.ruoyi.harness.api.VersionStatus;
import com.ruoyi.harness.core.port.HarnessVersionRepository;
import java.time.Instant;
import org.apache.ibatis.annotations.Param;

public interface HarnessVersionMapper extends HarnessVersionRepository {
    @Override int updateDraftSource(@Param("id")Long id,@Param("source")String source,@Param("status")VersionStatus status);
    @Override int updateValidation(@Param("id")Long id,@Param("sourceHash")String sourceHash,@Param("status")VersionStatus status,@Param("validatedAt")Instant validatedAt);
    @Override int updateStatus(@Param("id")Long id,@Param("status")VersionStatus status,@Param("publishedAt")Instant publishedAt);
}
