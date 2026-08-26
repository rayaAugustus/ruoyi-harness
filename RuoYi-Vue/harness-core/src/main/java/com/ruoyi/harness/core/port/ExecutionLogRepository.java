package com.ruoyi.harness.core.port;

import com.ruoyi.harness.core.domain.ExecutionLog;
import java.util.List;
import java.util.Map;

public interface ExecutionLogRepository { int insert(ExecutionLog log); List<ExecutionLog> search(Map<String,Object> filters); }
