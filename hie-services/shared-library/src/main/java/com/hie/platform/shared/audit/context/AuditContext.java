package com.hie.platform.shared.audit.context;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

    public static void setContext(AuditContext context) {
        contextHolder.set(context);
    }

    public static AuditContext getContext() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
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