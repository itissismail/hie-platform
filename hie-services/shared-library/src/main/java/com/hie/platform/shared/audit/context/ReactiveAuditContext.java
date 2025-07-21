package com.hie.platform.shared.audit.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Author M.Ismail
 * Reactive-aware audit context for Reactor streams
 * Date 15-July-2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactiveAuditContext {
    private UUID messageId;
    private UUID correlationId;
    private String serviceName;
    private String sourceOrganization;
    private String messageType;
    private String patientId;
    private String globalPatientId;
    private long startTime;

    public static final String CONTEXT_KEY = "AUDIT_CONTEXT";

    // Create context from current values
    public static ReactiveAuditContext create(UUID messageId, UUID correlationId, String serviceName) {
        return ReactiveAuditContext.builder()
                .messageId(messageId)
                .correlationId(correlationId)
                .serviceName(serviceName)
                .startTime(System.currentTimeMillis())
                .build();
    }

    // Add context to reactor context
    public static <T> Mono<T> withContext(Mono<T> mono, ReactiveAuditContext auditContext) {
        return mono.contextWrite(ctx -> ctx.put(CONTEXT_KEY, auditContext));
    }

    // Get context from reactor context
    public static Mono<ReactiveAuditContext> getCurrentContext() {
        return Mono.deferContextual(ctx -> {
            if (ctx.hasKey(CONTEXT_KEY)) {
                return Mono.just(ctx.get(CONTEXT_KEY));
            }
            return Mono.empty();
        });
    }

    // Extract messageId from context
    public static Mono<UUID> getCurrentMessageId() {
        return getCurrentContext()
                .map(ReactiveAuditContext::getMessageId);
    }

    // Extract correlationId from context
    public static Mono<UUID> getCurrentCorrelationId() {
        return getCurrentContext()
                .map(ReactiveAuditContext::getCorrelationId);
    }

    // Extract serviceName from context
    public static Mono<String> getCurrentServiceName() {
        return getCurrentContext()
                .map(ReactiveAuditContext::getServiceName);
    }

    // Helper to create context and add it to Reactor Context
    public Context toReactorContext() {
        return Context.of(CONTEXT_KEY, this);
    }

    // Helper to merge with existing Reactor Context
    public Context mergeToReactorContext(Context existingContext) {
        return existingContext.put(CONTEXT_KEY, this);
    }
}