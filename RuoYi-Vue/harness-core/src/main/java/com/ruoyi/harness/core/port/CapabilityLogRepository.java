package com.ruoyi.harness.core.port;

import com.ruoyi.harness.core.domain.CapabilityLog;
import java.util.List;
import java.util.Map;

public interface CapabilityLogRepository { int insert(CapabilityLog log); List<CapabilityLog> search(Map<String,Object> filters); }
