package com.ruoyi.harness.adapter.web;

import com.ruoyi.harness.api.*;
import com.ruoyi.harness.ai.model.HarnessAiException;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages="com.ruoyi.harness.adapter.web")
public class HarnessExceptionHandler {
    @ExceptionHandler(HarnessAiException.class) public ResponseEntity<HarnessError> handleAi(HarnessAiException e){HttpStatus status=switch(e.getCode()){
        case AI_SESSION_NOT_FOUND,AI_DRAFT_NOT_FOUND->HttpStatus.NOT_FOUND;
        case AI_SESSION_ACCESS_DENIED->HttpStatus.FORBIDDEN;
        case AI_RATE_LIMITED->HttpStatus.TOO_MANY_REQUESTS;
        case AI_TIMEOUT->HttpStatus.REQUEST_TIMEOUT;
        case AI_PROVIDER_UNAVAILABLE->HttpStatus.SERVICE_UNAVAILABLE;
        case AI_PROVIDER_AUTH_FAILED->HttpStatus.BAD_GATEWAY;
        case AI_DISABLED,AI_SESSION_ARCHIVED->HttpStatus.CONFLICT;
        default->HttpStatus.BAD_REQUEST;};
        return ResponseEntity.status(status).body(new HarnessError(e.getCode().name(),e.getMessage(),UUID.randomUUID().toString(),e.getDetails()));}
    @ExceptionHandler(HarnessException.class) public ResponseEntity<HarnessError> handle(HarnessException e){HttpStatus status=switch(e.getCode()){
        case APP_NOT_FOUND,APP_VERSION_NOT_FOUND,CAPABILITY_NOT_FOUND->HttpStatus.NOT_FOUND;
        case APP_ACCESS_DENIED,CAPABILITY_PERMISSION_DENIED,CAPABILITY_POLICY_DENIED->HttpStatus.FORBIDDEN;
        case APP_KEY_CONFLICT,CAPABILITY_CONFLICT,VERSION_IMMUTABLE,VERSION_STATE_INVALID,VERSION_OWNERSHIP_INVALID->HttpStatus.CONFLICT;
        case SCRIPT_TIMEOUT->HttpStatus.REQUEST_TIMEOUT;default->HttpStatus.BAD_REQUEST;};
        Object details=status==HttpStatus.FORBIDDEN?null:e.getDetails();return ResponseEntity.status(status).body(new HarnessError(e.getCode().name(),e.getMessage(),UUID.randomUUID().toString(),details));}
}
