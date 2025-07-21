package com.hie.platform.shared.audit.aspect;

import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.context.ReactiveAuditContext;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.audit.service.AuditTrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Author M.Ismail
 * Reactive AOP aspect that intercepts @AuditStep methods
 * Date 15-July-2025
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ReactiveAuditTrailAspect {

    private final AuditTrailService auditTrailService;

    @Around("@annotation(auditStep)")
    public Object auditStepExecution(ProceedingJoinPoint joinPoint, AuditStep auditStep) throws Throwable {
        String serviceName = auditStep.serviceName();
        MessageStatus stepName = auditStep.stepName();

        log.debug("Starting reactive audit step: {} for service: {}", stepName.getValue(), serviceName);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            // Handle reactive types
            if (result instanceof Mono) {
                return ((Mono<?>) result)
                        .flatMap(response -> handleReactiveAudit(response, serviceName, stepName, startTime, null))
                        .doOnError(error -> handleReactiveAudit(null, serviceName, stepName, startTime, error))
                        .onErrorResume(error -> Mono.error(error));
            } else {
                return handleNonReactiveAudit(result, serviceName, stepName, startTime, null);
            }
        } catch (Exception e) {
            return handleNonReactiveAudit(null, serviceName, stepName, startTime, e);
        }
    }

    private Mono<?> handleReactiveAudit(Object response, String serviceName, MessageStatus stepName,
                                        long startTime, Throwable error) {
        return ReactiveAuditContext.getCurrentContext()
                .switchIfEmpty(createFallbackContext(serviceName))
                .flatMap(context -> {
                    long processingTime = System.currentTimeMillis() - startTime;

                    if (error != null) {
                        log.error("Reactive audit step failed: {} with error: {}", stepName.getValue(), error.getMessage());
                        return auditTrailService.failStep(
                                context.getMessageId(),
                                context.getCorrelationId(),
                                serviceName,
                                stepName.getValue(),
                                error.getMessage(),
                                processingTime
                        ).thenReturn(response);
                    } else {
                        log.debug("Completing reactive audit step: {} with processing time: {}ms",
                                stepName.getValue(), processingTime);
                        return auditTrailService.completeStep(
                                context.getMessageId(),
                                context.getCorrelationId(),
                                serviceName,
                                stepName.getValue(),
                                response != null ? truncateResponse(response.toString()) : null,
                                processingTime
                        ).thenReturn(response);
                    }
                })
                .onErrorResume(auditError -> {
                    log.error("Error during audit logging, continuing with original operation", auditError);
                    return response != null ? Mono.just(response) : Mono.error(error);
                });
    }

    private Object handleNonReactiveAudit(Object response, String serviceName, MessageStatus stepName,
                                          long startTime, Throwable error) {
        // For non-reactive methods, we'll use a fallback context
        UUID messageId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        long processingTime = System.currentTimeMillis() - startTime;

        log.debug("Handling non-reactive audit for step: {} with fallback context", stepName.getValue());

        if (error != null) {
            auditTrailService.failStep(messageId, correlationId, serviceName,
                            stepName.getValue(), error.getMessage(), processingTime)
                    .subscribe(
                            null,
                            auditError -> log.error("Failed to log audit failure", auditError)
                    );
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new RuntimeException(error);
            }
        } else {
            auditTrailService.completeStep(messageId, correlationId, serviceName,
                            stepName.getValue(), response != null ? truncateResponse(response.toString()) : null, processingTime)
                    .subscribe(
                            null,
                            auditError -> log.error("Failed to log audit completion", auditError)
                    );
            return response;
        }
    }

    private Mono<ReactiveAuditContext> createFallbackContext(String serviceName) {
        log.debug("Creating fallback audit context for service: {}", serviceName);
        return Mono.just(ReactiveAuditContext.create(UUID.randomUUID(), UUID.randomUUID(), serviceName));
    }

    private String truncateResponse(String response) {
        if (response == null) return null;
        return response.length() > 500 ? response.substring(0, 500) + "... (truncated)" : response;
    }

    private String extractRequestPayload(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            // Skip ServerHttpRequest objects
            if (arg instanceof ServerHttpRequest) {
                continue;
            }
            // Handle Mono<String> for reactive request bodies
            if (arg instanceof Mono) {
                return "Reactive request body (Mono)";
            }
            // Handle String arguments (likely the HL7 message)
            if (arg instanceof String) {
                String payload = (String) arg;
                // Truncate long payloads for logging
                if (payload.length() > 200) {
                    return payload.substring(0, 200) + "... (truncated)";
                }
                return payload;
            }
            // Handle other argument types
            if (arg != null && !(arg instanceof UUID)) {
                return arg.toString();
            }
        }
        return null;
    }
}