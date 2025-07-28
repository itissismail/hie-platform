package com.hie.platform.shared.audit.aspect;

import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.context.NonReactiveAuditContext;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.audit.service.NonReactiveAuditTrailService;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Author M.Ismail
 * Non-reactive AOP aspect that intercepts @AuditStep methods for message-based services
 * Date 28-July-2025
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Execute before other aspects
public class NonReactiveAuditTrailAspect {

    private final NonReactiveAuditTrailService nonReactiveAuditTrailService;

    @Around("@annotation(auditStep) && !execution(* *..*.*(..reactor.core.publisher.Mono..))")
    public Object auditNonReactiveStepExecution(ProceedingJoinPoint joinPoint, AuditStep auditStep) throws Throwable {
        String serviceName = auditStep.serviceName();
        MessageStatus stepName = auditStep.stepName();

        log.debug("Starting non-reactive audit step: {} for service: {}", stepName.getValue(), serviceName);

        long startTime = System.currentTimeMillis();
        UUID newMessageId = null;
        UUID correlationId = null;
        UUID previousMessageId = null;

        try {
            // Extract QueueMessage from method arguments to get IDs
            QueueMessage queueMessage = extractQueueMessage(joinPoint);

            if (queueMessage != null) {
                correlationId = UUID.fromString(queueMessage.getCorrelationId());
                previousMessageId = UUID.fromString(queueMessage.getMessageId());

                // Generate new message ID for this service
                newMessageId = UUID.randomUUID();

                // Set up non-reactive audit context
                NonReactiveAuditContext.setContext(NonReactiveAuditContext.builder()
                        .messageId(newMessageId)
                        .correlationId(correlationId)
                        .previousMessageId(previousMessageId)
                        .serviceName(serviceName)
                        .stepName(stepName.getValue())
                        .startTime(startTime)
                        .build());

                // Update the QueueMessage with new message ID for downstream processing
                queueMessage.setMessageId(newMessageId.toString());

                // Start audit step
                nonReactiveAuditTrailService.startStepSync(correlationId, previousMessageId,
                        serviceName, stepName.getValue(), extractRequestPayload(joinPoint));

                log.debug("Started audit for step: {}, newMessageId: {}, correlationId: {}, previousMessageId: {}",
                        stepName.getValue(), newMessageId, correlationId, previousMessageId);
            } else {
                log.warn("No QueueMessage found in method arguments for audit step: {}", stepName.getValue());
                // Fallback: create context with generated IDs
                newMessageId = UUID.randomUUID();
                correlationId = UUID.randomUUID();

                NonReactiveAuditContext.setContext(NonReactiveAuditContext.builder()
                        .messageId(newMessageId)
                        .correlationId(correlationId)
                        .serviceName(serviceName)
                        .stepName(stepName.getValue())
                        .startTime(startTime)
                        .build());
            }

            // Execute the original method
            Object result = joinPoint.proceed();

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;

            // Complete audit step
            if (newMessageId != null) {
                nonReactiveAuditTrailService.completeStep(newMessageId, correlationId,
                        serviceName, stepName.getValue(),
                        result != null ? truncateResponse(result.toString()) : null, processingTime);

                log.debug("Completed audit step: {} with processing time: {}ms",
                        stepName.getValue(), processingTime);
            }

            return result;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            log.error("Non-reactive audit step failed: {} with error: {}", stepName.getValue(), e.getMessage());

            // Fail audit step
            if (newMessageId != null && correlationId != null) {
                nonReactiveAuditTrailService.failStep(newMessageId, correlationId,
                        serviceName, stepName.getValue(), e.getMessage(), processingTime);
            }

            throw e; // Re-throw the original exception

        } finally {
            // Clean up context
            NonReactiveAuditContext.clear();
        }
    }

    /**
     * Extract QueueMessage from method arguments
     */
    private QueueMessage extractQueueMessage(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof QueueMessage) {
                return (QueueMessage) arg;
            }
        }
        return null;
    }

    /**
     * Extract request payload from method arguments
     */
    private String extractRequestPayload(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof QueueMessage) {
                QueueMessage queueMessage = (QueueMessage) arg;
                if (queueMessage.getPayload() != null) {
                    return queueMessage.getPayload().toString();
                }
            }
            if (arg instanceof String && ((String) arg).length() < 500) {
                return (String) arg;
            }
        }
        return null;
    }

    /**
     * Truncate response for logging
     */
    private String truncateResponse(String response) {
        if (response == null) return null;
        return response.length() > 500 ? response.substring(0, 500) + "... (truncated)" : response;
    }
}