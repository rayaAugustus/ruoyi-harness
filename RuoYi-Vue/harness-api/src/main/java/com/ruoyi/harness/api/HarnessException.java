package com.ruoyi.harness.api;

public class HarnessException extends RuntimeException {
    private final HarnessErrorCode code;
    private final Object details;

    public HarnessException(HarnessErrorCode code, String message) { this(code, message, null, null); }
    public HarnessException(HarnessErrorCode code, String message, Throwable cause) { this(code, message, null, cause); }
    public HarnessException(HarnessErrorCode code, String message, Object details, Throwable cause) {
        super(message, cause); this.code = code; this.details = details;
    }
    public HarnessErrorCode getCode() { return code; }
    public Object getDetails() { return details; }
}
