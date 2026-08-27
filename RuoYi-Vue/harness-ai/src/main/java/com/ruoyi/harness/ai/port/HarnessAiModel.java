package com.ruoyi.harness.ai.port;

import com.ruoyi.harness.ai.model.AiGenerationRequest;
import com.ruoyi.harness.ai.model.AiGenerationResult;

public interface HarnessAiModel { AiGenerationResult generate(AiGenerationRequest request); }
