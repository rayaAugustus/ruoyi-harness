package com.ruoyi.harness.adapter.web;

import com.ruoyi.harness.api.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages="com.ruoyi.harness.adapter.web")
public class HarnessExceptionHandler {
    @ExceptionHandler(HarnessException.class) public ResponseEntity<HarnessError> handle(HarnessException e){HttpStatus status=switch(e.getCode()){
        case APP_NOT_FOUND,APP_VERSION_NOT_FOUND,CAPABILITY_NOT_FOUND->HttpStatus.NOT_FOUND;
        case APP_ACCESS_DENIED,CAPABILITY_PERMISSION_DENIED,CAPABILITY_POLICY_DENIED->HttpStatus.FORBIDDEN;
        case APP_KEY_CONFLICT,CAPABILITY_CONFLICT,VERSION_IMMUTABLE,VERSION_STATE_INVALID,VERSION_OWNERSHIP_INVALID->HttpStatus.CONFLICT;
        case SCRIPT_TIMEOUT->HttpStatus.REQUEST_TIMEOUT;default->HttpStatus.BAD_REQUEST;};
        Object details=status==HttpStatus.FORBIDDEN?null:e.getDetails();return ResponseEntity.status(status).body(new HarnessError(e.getCode().name(),e.getMessage(),UUID.randomUUID().toString(),details));}
}
