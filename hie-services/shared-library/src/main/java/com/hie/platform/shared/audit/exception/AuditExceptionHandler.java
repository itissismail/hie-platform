package com.hie.platform.shared.audit.exception;

import com.hie.platform.shared.audit.context.AuditContext;
import com.hie.platform.shared.audit.service.AuditTrailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class AuditExceptionHandler {

    @Autowired
    private AuditTrailService auditTrailService;

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception e) {
        AuditContext context = AuditContext.getContext();

        if (context != null) {
            auditTrailService.failStep(
                    context.getMessageId(),
                    context.getCorrelationId(),
                    context.getServiceName(),
                    "error-handling",
                    e.getMessage(),
                    System.currentTimeMillis() - context.getStartTime()
            ).subscribe();
        }

        ErrorResponse error = new ErrorResponse(e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}