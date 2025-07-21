package com.hie.platform.shared.audit.exception;

import com.hie.platform.shared.audit.context.AuditContext;
import com.hie.platform.shared.audit.service.AuditTrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class AuditExceptionHandler {

    @Autowired
    private AuditTrailService auditTrailService;

    // Add a flag to prevent recursive calls
    private static final ThreadLocal<Boolean> HANDLING_EXCEPTION = new ThreadLocal<>();

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception e) {
        // Prevent recursive exception handling
        if (Boolean.TRUE.equals(HANDLING_EXCEPTION.get())) {
            log.error("Recursive exception detected in audit handler, returning simple error", e);
            ErrorResponse error = new ErrorResponse("Internal server error occurred");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
        }

        try {
            HANDLING_EXCEPTION.set(true);
            log.error("Exception caught in AuditExceptionHandler", e);

            AuditContext context = AuditContext.getContext();

            if (context != null && context.getMessageId() != null && context.getCorrelationId() != null) {
                log.debug("Audit context found, recording failure step");

                // Use subscribe with error handling to prevent recursive calls
                auditTrailService.failStep(
                        context.getMessageId(),
                        context.getCorrelationId(),
                        context.getServiceName() != null ? context.getServiceName() : "unknown-service",
                        "error-handling",
                        e.getMessage(),
                        System.currentTimeMillis() - (context.getStartTime() != null ? context.getStartTime() : System.currentTimeMillis())
                ).doOnError(auditError -> {
                    log.warn("Failed to record audit failure step", auditError);
                }).subscribe();
            } else {
                log.debug("No valid audit context found, skipping audit step recording");
            }

            ErrorResponse error = new ErrorResponse(e.getMessage() != null ? e.getMessage() : "An error occurred");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));

        } catch (Exception handlerException) {
            log.error("Exception in exception handler itself", handlerException);
            ErrorResponse error = new ErrorResponse("Internal server error occurred");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
        } finally {
            HANDLING_EXCEPTION.remove();
        }
    }

    // Add specific handler for audit-related exceptions to prevent recursion
    @ExceptionHandler(AuditException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuditException(AuditException e) {
        log.error("Audit-specific exception caught", e);
        ErrorResponse error = new ErrorResponse("Audit system error: " + e.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}