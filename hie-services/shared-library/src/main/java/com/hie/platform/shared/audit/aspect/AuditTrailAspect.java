package com.hie.platform.shared.audit.aspect;


import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.service.AuditTrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 *  Author M.Ismail
 *  AOP aspect that intercepts @AuditStep methods
 *  Date 15-July-2025
 */

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditTrailAspect {

    private final AuditTrailService auditTrailService;

    @Around("@annotation(auditStep)")
    public Object auditStepExecution(ProceedingJoinPoint joinPoint, AuditStep auditStep) throws Throwable {
        UUID messageId = extractMessageId(joinPoint);
        UUID correlationId = extractCorrelationId(joinPoint);
        String serviceName = auditStep.serviceName();
        String stepName = auditStep.stepName();

        long startTime = System.currentTimeMillis();

        // Start audit step
        auditTrailService.startStep(messageId, correlationId, serviceName, stepName,
                extractRequestPayload(joinPoint)).subscribe();

        try {
            Object result = joinPoint.proceed();

            // Handle reactive types
            if (result instanceof Mono) {
                return ((Mono<?>) result)
                        .doOnSuccess(response -> {
                            long processingTime = System.currentTimeMillis() - startTime;
                            auditTrailService.completeStep(messageId, correlationId, serviceName,
                                            stepName, response != null ? response.toString() : null, processingTime)
                                    .subscribe();
                        })
                        .doOnError(error -> {
                            long processingTime = System.currentTimeMillis() - startTime;
                            auditTrailService.failStep(messageId, correlationId, serviceName,
                                            stepName, error.getMessage(), processingTime)
                                    .subscribe();
                        });
            } else {
                // Complete audit step for non-reactive types
                long processingTime = System.currentTimeMillis() - startTime;
                auditTrailService.completeStep(messageId, correlationId, serviceName,
                                stepName, result != null ? result.toString() : null, processingTime)
                        .subscribe();
                return result;
            }
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            auditTrailService.failStep(messageId, correlationId, serviceName,
                            stepName, e.getMessage(), processingTime)
                    .subscribe();
            throw e;
        }
    }

    private UUID extractMessageId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
            // Add more extraction logic based on your DTOs
        }
        return UUID.randomUUID(); // Fallback
    }

    private UUID extractCorrelationId(ProceedingJoinPoint joinPoint) {
        // Extract correlation ID from request context or arguments
        // This depends on your specific implementation
        return UUID.randomUUID(); // Fallback
    }

    private String extractRequestPayload(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            return args[0].toString();
        }
        return null;
    }
}
