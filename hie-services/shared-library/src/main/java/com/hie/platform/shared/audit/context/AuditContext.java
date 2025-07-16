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
            return Mono.justOrEmpty(getContext());
        });
    }

    public static UUID getMessageId() {
        AuditContext context = getContext();
        return context != null ? context.getMessageId() : null;
    }

    public static UUID getCorrelationId() {
        AuditContext context = getContext();
        return context != null ? context.getCorrelationId() : null;
    }

    public static String getServiceName() {
        AuditContext context = getContext();
        return context != null ? context.getServiceName() : null;
    }
}