package com.ruoyi.harness.ai.port;

import com.ruoyi.harness.ai.model.AiStoredMessage;
import java.util.List;

public interface AiMessageRepository { List<AiStoredMessage> findBySessionId(Long sessionId); int insert(AiStoredMessage message); }
