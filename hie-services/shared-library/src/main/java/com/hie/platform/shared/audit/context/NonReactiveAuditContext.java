package com.hie.platform.shared.audit.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Author M.Ismail
 * Non-reactive audit context for message-based services using ThreadLocal
 * Date 28-July-2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NonReactiveAuditContext {

    private UUID messageId;
    private UUID correlationId;
    private UUID previousMessageId;
    private String serviceName;
    private String stepName;
    private String sourceOrganization;
    private String messageType;
    private String patientId;
    private String globalPatientId;
    private long startTime;

    private static final ThreadLocal<NonReactiveAuditContext> contextHolder = new ThreadLocal<>();

    /**
     * Set audit context for current thread
     */
    public static void setContext(NonReactiveAuditContext context) {
        contextHolder.set(context);
        if (context != null) {
            log.debug("Set non-reactive audit context - MessageId: {}, CorrelationId: {}, PreviousMessageId: {}",
                    context.getMessageId(), context.getCorrelationId(), context.getPreviousMessageId());
        }
    }

    /**
     * Get audit context from current thread
     */
    public static NonReactiveAuditContext getContext() {
        return contextHolder.get();
    }

    /**
     * Clear audit context from current thread
     */
    public static void clear() {
        NonReactiveAuditContext context = contextHolder.get();
        if (context != null) {
            log.debug("Clearing non-reactive audit context - MessageId: {}", context.getMessageId());
        }
        contextHolder.remove();
    }

    /**
     * Create new context with IDs
     */
    public static NonReactiveAuditContext create(UUID messageId, UUID correlationId,
                                                 UUID previousMessageId, String serviceName) {
        return NonReactiveAuditContext.builder()
                .messageId(messageId)
                .correlationId(correlationId)
                .previousMessageId(previousMessageId)
                .serviceName(serviceName)
                .startTime(System.currentTimeMillis())
                .build();
    }

    // Convenience methods to get current context values
    public static UUID getCurrentMessageId() {
        NonReactiveAuditContext context = getContext();
        return context != null ? context.messageId : null;
    }

    public static UUID getCurrentCorrelationId() {
        NonReactiveAuditContext context = getContext();
        return context != null ? context.correlationId : null;
    }

    public static UUID getCurrentPreviousMessageId() {
        NonReactiveAuditContext context = getContext();
        return context != null ? context.previousMessageId : null;
    }

    public static String getCurrentServiceName() {
        NonReactiveAuditContext context = getContext();
        return context != null ? context.serviceName : null;
    }

    public static String getCurrentStepName() {
        NonReactiveAuditContext context = getContext();
        return context != null ? context.stepName : null;
    }

    public static long getCurrentStartTime() {
        NonReactiveAuditContext context = getContext();
        return context != null ? context.startTime : 0L;
    }

    // Helper method to check if context exists
    public static boolean hasContext() {
        return getContext() != null;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NonReactiveAuditContext.class);
}