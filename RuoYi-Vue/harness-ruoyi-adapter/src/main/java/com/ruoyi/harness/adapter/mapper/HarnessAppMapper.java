package com.ruoyi.harness.adapter.mapper;

import com.ruoyi.harness.core.port.HarnessAppRepository;
import org.apache.ibatis.annotations.Param;

public interface HarnessAppMapper extends HarnessAppRepository {
    @Override int setEnabled(@Param("id") Long id,@Param("enabled") boolean enabled,@Param("actorId") Long actorId);
    @Override int setPublishedVersion(@Param("id") Long id,@Param("versionId") Long versionId,@Param("actorId") Long actorId);
}
