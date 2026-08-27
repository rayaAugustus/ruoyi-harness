package com.ruoyi.harness.ai.model;

public final class HarnessAiException extends RuntimeException {
    private final AiErrorCode code;
    private final Object details;
    public HarnessAiException(AiErrorCode code, String message) { this(code, message, null, null); }
    public HarnessAiException(AiErrorCode code, String message, Throwable cause) { this(code, message, null, cause); }
    public HarnessAiException(AiErrorCode code, String message, Object details, Throwable cause) {
        super(message, cause); this.code = code; this.details = details;
    }
    public AiErrorCode getCode() { return code; }
    public Object getDetails() { return details; }
}
