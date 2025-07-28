package com.hie.platform.shared.audit.aspect;

import com.hie.platform.shared.audit.annotation.NonReactiveAuditStep;
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

import java.util.Map;
import java.util.UUID;

/**
 * Author M.Ismail
 * Non-reactive AOP aspect that intercepts @NonReactiveAuditStep methods for message-based services
 * Date 28-July-2025
 *
 * UPDATED: Enhanced to handle both @AuditStep and @NonReactiveAuditStep annotations
 * - Uses existing correlationId from QueueMessage
 * - Generates new messageId for current service processing (configurable)
 * - Preserves previous messageId from incoming message
 * - Properly handles message-based service audit flow
 * - Uses ThreadLocal context for non-reactive operations
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Execute before other aspects
public class NonReactiveAuditTrailAspect {

    private final NonReactiveAuditTrailService nonReactiveAuditTrailService;

    /**
     * Handle @NonReactiveAuditStep annotation
     */
    @Around("@annotation(nonReactiveAuditStep)")
    public Object auditNonReactiveStepExecution(ProceedingJoinPoint joinPoint, NonReactiveAuditStep nonReactiveAuditStep) throws Throwable {
        String serviceName = nonReactiveAuditStep.serviceName();
        MessageStatus stepName = nonReactiveAuditStep.stepName();
        boolean generateNewMessageId = nonReactiveAuditStep.generateNewMessageId();
        boolean extractFromQueueMessage = nonReactiveAuditStep.extractFromQueueMessage();

        log.debug("Starting non-reactive audit step: {} for service: {}, generateNewMessageId: {}",
                stepName.getValue(), serviceName, generateNewMessageId);

        long startTime = System.currentTimeMillis();
        UUID messageId = null;
        UUID correlationId = null;
        UUID previousMessageId = null;

        try {
            // Extract QueueMessage from method arguments if enabled
            QueueMessage queueMessage = null;
            if (extractFromQueueMessage) {
                queueMessage = extractQueueMessage(joinPoint);
            }

            if (queueMessage != null) {
                // Use existing correlationId from QueueMessage (preserve throughout flow)
                correlationId = UUID.fromString(queueMessage.getCorrelationId());

                if (generateNewMessageId) {
                    // Get current messageId as previousMessageId
                    previousMessageId = UUID.fromString(queueMessage.getMessageId());

                    // Generate NEW messageId for current service processing
                    messageId = UUID.randomUUID();

                    // Update QueueMessage with new messageId for current service
                    queueMessage.setMessageId(messageId.toString());

                    // Store previousMessageId in payload for downstream services
                    Map<String, Object> payload = queueMessage.getPayload();
                    if (payload != null) {
                        payload.put("previousMessageId", previousMessageId.toString());
                    }

                    log.debug("Message ID flow - CorrelationId: {} (preserved), PreviousMessageId: {} (from incoming), NewMessageId: {} (generated for current service)",
                            correlationId, previousMessageId, messageId);
                } else {
                    // Use existing messageId without generating new one
                    messageId = UUID.fromString(queueMessage.getMessageId());

                    // Try to get previousMessageId from payload
                    Map<String, Object> payload = queueMessage.getPayload();
                    if (payload != null && payload.containsKey("previousMessageId")) {
                        try {
                            previousMessageId = UUID.fromString((String) payload.get("previousMessageId"));
                        } catch (Exception e) {
                            log.debug("Could not parse previousMessageId from payload: {}", payload.get("previousMessageId"));
                        }
                    }

                    log.debug("Using existing messageId: {}, correlationId: {}, previousMessageId: {}",
                            messageId, correlationId, previousMessageId);
                }

                // Set up non-reactive audit context with proper ID relationships
                NonReactiveAuditContext.setContext(NonReactiveAuditContext.builder()
                        .messageId(messageId)
                        .correlationId(correlationId)
                        .previousMessageId(previousMessageId)
                        .serviceName(serviceName)
                        .stepName(stepName.getValue())
                        .startTime(startTime)
                        .build());

                // Start audit step with proper ID relationships
                nonReactiveAuditTrailService.startStepWithExistingMessageId(
                        messageId,
                        correlationId,
                        previousMessageId,
                        serviceName,
                        stepName.getValue(),
                        extractRequestPayload(joinPoint)
                );

                log.debug("Started audit for step: {}, messageId: {}, correlationId: {}, previousMessageId: {}",
                        stepName.getValue(), messageId, correlationId, previousMessageId);

            } else {
                log.warn("No QueueMessage found in method arguments for audit step: {} - creating fallback context",
                        stepName.getValue());

                // Fallback: create context with generated IDs (should not happen in message-based services)
                messageId = UUID.randomUUID();
                correlationId = UUID.randomUUID();

                NonReactiveAuditContext.setContext(NonReactiveAuditContext.builder()
                        .messageId(messageId)
                        .correlationId(correlationId)
                        .serviceName(serviceName)
                        .stepName(stepName.getValue())
                        .startTime(startTime)
                        .build());

                // Start audit step even without proper context
                nonReactiveAuditTrailService.startStepWithExistingMessageId(
                        messageId, correlationId, null,
                        serviceName, stepName.getValue(), extractRequestPayload(joinPoint));
            }

            // Execute the original method
            Object result = joinPoint.proceed();

            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;

            // Complete audit step
            if (messageId != null && correlationId != null) {
                nonReactiveAuditTrailService.completeStep(messageId, correlationId,
                        serviceName, stepName.getValue(),
                        result != null ? truncateResponse(result.toString()) : null, processingTime);

                log.debug("Completed audit step: {} with processing time: {}ms, messageId: {}",
                        stepName.getValue(), processingTime, messageId);
            }

            return result;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            log.error("Non-reactive audit step failed: {} with error: {}, messageId: {}",
                    stepName.getValue(), e.getMessage(), messageId);

            // Fail audit step
            if (messageId != null && correlationId != null) {
                nonReactiveAuditTrailService.failStep(messageId, correlationId,
                        serviceName, stepName.getValue(), e.getMessage(), processingTime);
            }

            throw e; // Re-throw the original exception

        } finally {
            // Clean up context
            NonReactiveAuditContext.clear();
        }
    }

    /**
     * Handle legacy @AuditStep annotation for backward compatibility
     * UPDATED: Added support for both annotations
     */
    @Around("@annotation(auditStep) && !execution(* *..*.*(..reactor.core.publisher.Mono..))")
    public Object auditLegacyStepExecution(ProceedingJoinPoint joinPoint, com.hie.platform.shared.audit.annotation.AuditStep auditStep) throws Throwable {
        // Convert legacy annotation to new format
        NonReactiveAuditStep converted = new NonReactiveAuditStep() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return NonReactiveAuditStep.class;
            }

            @Override
            public String serviceName() {
                return auditStep.serviceName();
            }

            @Override
            public MessageStatus stepName() {
                return auditStep.stepName();
            }

            @Override
            public boolean generateNewMessageId() {
                return true; // Default behavior for legacy annotation
            }

            @Override
            public boolean extractFromQueueMessage() {
                return true; // Default behavior for legacy annotation
            }
        };

        return auditNonReactiveStepExecution(joinPoint, converted);
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
     * Extract request payload from method arguments for audit logging
     */
    private String extractRequestPayload(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof QueueMessage) {
                QueueMessage queueMessage = (QueueMessage) arg;
                if (queueMessage.getPayload() != null) {
                    // Create a clean payload summary for audit (remove sensitive data if needed)
                    Map<String, Object> payload = queueMessage.getPayload();
                    StringBuilder summary = new StringBuilder();
                    summary.append("QueueMessage{");
                    summary.append("messageId=").append(queueMessage.getMessageId());
                    summary.append(", correlationId=").append(queueMessage.getCorrelationId());

                    if (payload.containsKey("organizationId")) {
                        summary.append(", organizationId=").append(payload.get("organizationId"));
                    }
                    if (payload.containsKey("hl7MessageType")) {
                        summary.append(", hl7MessageType=").append(payload.get("hl7MessageType"));
                    }
                    if (payload.containsKey("patientId")) {
                        summary.append(", patientId=").append(payload.get("patientId"));
                    }
                    if (payload.containsKey("minioPath")) {
                        summary.append(", minioPath=").append(payload.get("minioPath"));
                    }
                    if (payload.containsKey("previousMessageId")) {
                        summary.append(", previousMessageId=").append(payload.get("previousMessageId"));
                    }
                    summary.append("}");

                    return summary.toString();
                }
            }
            // Handle other string arguments (but limit size)
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