package com.hie.platform.shared.audit.context;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Author M.Ismail
 * Class-level annotation for automatic audit logging
 * Date 15-July-2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditContext {
    private UUID messageId;
    private UUID correlationId;
    private String serviceName;
    private String sourceOrganization;
    private String messageType;
    private String patientId;
    private String globalPatientId;
    private long startTime;

    private static final ThreadLocal<AuditContext> contextHolder = new ThreadLocal<>();

    // Add reactor context support
    public static final String REACTOR_CONTEXT_KEY = "AUDIT_CONTEXT";

    public static void setContext(AuditContext context) {
        contextHolder.set(context);
    }

    public static AuditContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }

    // Reactor context methods
    public static Mono<AuditContext> getContextFromReactor() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(REACTOR_CONTEXT_KEY)) {
                return Mono.just(ctx.get(REACTOR_CONTEXT_KEY));
            }
            AuditContext threadLocalContext = getContext();
            return threadLocalContext != null ?
                    Mono.just(threadLocalContext) :
                    Mono.empty();
        });
    }

    // OPTION 1: Direct field access (safest approach)
    public static UUID getCurrentMessageId() {
        AuditContext context = getContext();
        return context != null ? context.messageId : null;
    }

    public static UUID getCurrentCorrelationId() {
        AuditContext context = getContext();
        return context != null ? context.correlationId : null;
    }

    public static String getCurrentServiceName() {
        AuditContext context = getContext();
        return context != null ? context.serviceName : null;
    }

    public static String getCurrentSourceOrganization() {
        AuditContext context = getContext();
        return context != null ? context.sourceOrganization : null;
    }

    public static String getCurrentMessageType() {
        AuditContext context = getContext();
        return context != null ? context.messageType : null;
    }

    public static String getCurrentPatientId() {
        AuditContext context = getContext();
        return context != null ? context.patientId : null;
    }

    public static String getCurrentGlobalPatientId() {
        AuditContext context = getContext();
        return context != null ? context.globalPatientId : null;
    }

    public static long getCurrentStartTime() {
        AuditContext context = getContext();
        return context != null ? context.startTime : 0L;
    }
}