package com.hie.platform.shared.audit.aspect;

import com.hie.platform.shared.audit.annotation.AuditStep;
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
 * AOP aspect that intercepts @AuditStep methods
 * Date 15-July-2025
 */

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditTrailAspect {

    private final AuditTrailService auditTrailService;

    // Thread-local storage for sharing IDs between controller and service
    private static final ThreadLocal<UUID> messageIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<UUID> correlationIdHolder = new ThreadLocal<>();

    @Around("@annotation(auditStep)")
    public Object auditStepExecution(ProceedingJoinPoint joinPoint, AuditStep auditStep) throws Throwable {
        UUID messageId = extractMessageId(joinPoint);
        UUID correlationId = extractCorrelationId(joinPoint);
        String serviceName = auditStep.serviceName();
        MessageStatus stepName = auditStep.stepName();

        log.debug("Starting audit step: {} for service: {} with messageId: {}, correlationId: {}",
                stepName.getValue(), serviceName, messageId, correlationId);

        long startTime = System.currentTimeMillis();

        // Start audit step
        auditTrailService.startStep(messageId, correlationId, serviceName, stepName.getValue(),
                extractRequestPayload(joinPoint)).subscribe();

        try {
            Object result = joinPoint.proceed();

            // Handle reactive types
            if (result instanceof Mono) {
                return ((Mono<?>) result)
                        .doOnSuccess(response -> {
                            long processingTime = System.currentTimeMillis() - startTime;
                            log.debug("Completing audit step: {} with processing time: {}ms",
                                    stepName.getValue(), processingTime);
                            auditTrailService.completeStep(messageId, correlationId, serviceName,
                                            stepName.getValue(), response != null ? response.toString() : null, processingTime)
                                    .subscribe();
                        })
                        .doOnError(error -> {
                            long processingTime = System.currentTimeMillis() - startTime;
                            log.error("Failing audit step: {} with error: {}", stepName.getValue(), error.getMessage());
                            auditTrailService.failStep(messageId, correlationId, serviceName,
                                            stepName.getValue(), error.getMessage(), processingTime)
                                    .subscribe();
                        })
                        .doFinally(signalType -> {
                            // Clear thread-local storage after processing
                            if (stepName == MessageStatus.RECEIVED) {
                                clearContext();
                            }
                        });
            } else {
                // Complete audit step for non-reactive types
                long processingTime = System.currentTimeMillis() - startTime;
                log.debug("Completing non-reactive audit step: {} with processing time: {}ms",
                        stepName.getValue(), processingTime);
                auditTrailService.completeStep(messageId, correlationId, serviceName,
                                stepName.getValue(), result != null ? result.toString() : null, processingTime)
                        .subscribe();
                return result;
            }
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Exception in audit step: {} with error: {}", stepName.getValue(), e.getMessage());
            auditTrailService.failStep(messageId, correlationId, serviceName,
                            stepName.getValue(), e.getMessage(), processingTime)
                    .subscribe();
            throw e;
        }
    }

    private UUID extractMessageId(ProceedingJoinPoint joinPoint) {
        // First try to get from thread-local storage (for service methods)
        UUID contextMessageId = messageIdHolder.get();
        if (contextMessageId != null) {
            log.debug("Using message ID from context: {}", contextMessageId);
            return contextMessageId;
        }

        // Try to extract from ServerHttpRequest headers (for controller methods)
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof ServerHttpRequest) {
                ServerHttpRequest request = (ServerHttpRequest) arg;
                String messageIdHeader = request.getHeaders().getFirst("X-Message-ID");
                if (messageIdHeader != null) {
                    try {
                        UUID messageId = UUID.fromString(messageIdHeader);
                        // Store in thread-local for service methods
                        messageIdHolder.set(messageId);
                        log.debug("Extracted message ID from header: {}", messageId);
                        return messageId;
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid message ID format in header: {}", messageIdHeader);
                    }
                }
            }
        }

        // Try to find UUID in method arguments
        for (Object arg : args) {
            if (arg instanceof UUID) {
                UUID messageId = (UUID) arg;
                messageIdHolder.set(messageId);
                log.debug("Found message ID in arguments: {}", messageId);
                return messageId;
            }
        }

        // Generate new UUID as fallback
        UUID messageId = UUID.randomUUID();
        messageIdHolder.set(messageId);
        log.debug("Generated new message ID: {}", messageId);
        return messageId;
    }

    private UUID extractCorrelationId(ProceedingJoinPoint joinPoint) {
        // First try to get from thread-local storage (for service methods)
        UUID contextCorrelationId = correlationIdHolder.get();
        if (contextCorrelationId != null) {
            log.debug("Using correlation ID from context: {}", contextCorrelationId);
            return contextCorrelationId;
        }

        // Try to extract from ServerHttpRequest headers (for controller methods)
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof ServerHttpRequest) {
                ServerHttpRequest request = (ServerHttpRequest) arg;
                String correlationIdHeader = request.getHeaders().getFirst("X-Correlation-ID");
                if (correlationIdHeader != null) {
                    try {
                        UUID correlationId = UUID.fromString(correlationIdHeader);
                        // Store in thread-local for service methods
                        correlationIdHolder.set(correlationId);
                        log.debug("Extracted correlation ID from header: {}", correlationId);
                        return correlationId;
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid correlation ID format in header: {}", correlationIdHeader);
                    }
                }
            }
        }

        // Generate new UUID as fallback
        UUID correlationId = UUID.randomUUID();
        correlationIdHolder.set(correlationId);
        log.debug("Generated new correlation ID: {}", correlationId);
        return correlationId;
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
                return "Reactive request body (Mono<String>)";
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

    private void clearContext() {
        messageIdHolder.remove();
        correlationIdHolder.remove();
        log.debug("Cleared audit context");
    }
}