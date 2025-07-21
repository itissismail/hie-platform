package com.hie.platform.shared.audit.filter;

import com.hie.platform.shared.audit.context.ReactiveAuditContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Author M.Ismail
 * Reactive WebFilter to extract and set audit context from HTTP headers
 * Date 15-July-2025
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ReactiveAuditContextFilter implements WebFilter {

    private static final String MESSAGE_ID_HEADER = "X-Message-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SOURCE_ORGANIZATION_HEADER = "X-Source-Organization";
    private static final String MESSAGE_TYPE_HEADER = "X-Message-Type";
    private static final String PATIENT_ID_HEADER = "X-Patient-ID";
    private static final String GLOBAL_PATIENT_ID_HEADER = "X-Global-Patient-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return extractAuditContext(exchange)
                .flatMap(auditContext -> {
                    log.debug("Extracted audit context for messageId: {}, correlationId: {}",
                            auditContext.getMessageId(), auditContext.getCorrelationId());

                    // Add response headers for downstream services
                    exchange.getResponse().getHeaders().add(MESSAGE_ID_HEADER, auditContext.getMessageId().toString());
                    exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, auditContext.getCorrelationId().toString());

                    if (auditContext.getServiceName() != null) {
                        exchange.getResponse().getHeaders().add(SERVICE_NAME_HEADER, auditContext.getServiceName());
                    }

                    return chain.filter(exchange)
                            .contextWrite(auditContext.toReactorContext());
                })
                .doOnError(error -> log.error("Error in audit context filter", error))
                .doFinally(signalType -> log.debug("Completed request processing with signal: {}", signalType));
    }

    private Mono<ReactiveAuditContext> extractAuditContext(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> {
            String messageIdStr = exchange.getRequest().getHeaders().getFirst(MESSAGE_ID_HEADER);
            String correlationIdStr = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            String serviceName = exchange.getRequest().getHeaders().getFirst(SERVICE_NAME_HEADER);
            String sourceOrganization = exchange.getRequest().getHeaders().getFirst(SOURCE_ORGANIZATION_HEADER);
            String messageType = exchange.getRequest().getHeaders().getFirst(MESSAGE_TYPE_HEADER);
            String patientId = exchange.getRequest().getHeaders().getFirst(PATIENT_ID_HEADER);
            String globalPatientId = exchange.getRequest().getHeaders().getFirst(GLOBAL_PATIENT_ID_HEADER);

            // Generate UUIDs if not provided
            UUID messageId;
            UUID correlationId;

            try {
                messageId = messageIdStr != null ? UUID.fromString(messageIdStr) : UUID.randomUUID();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid messageId format: {}, generating new one", messageIdStr);
                messageId = UUID.randomUUID();
            }

            try {
                correlationId = correlationIdStr != null ? UUID.fromString(correlationIdStr) : UUID.randomUUID();
            } catch (IllegalArgumentException e) {
                log.warn("Invalid correlationId format: {}, generating new one", correlationIdStr);
                correlationId = UUID.randomUUID();
            }

            // Extract service name from request path if not provided in header
            if (serviceName == null || serviceName.trim().isEmpty()) {
                String path = exchange.getRequest().getPath().value();
                serviceName = extractServiceNameFromPath(path);
            }

            return ReactiveAuditContext.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .serviceName(serviceName)
                    .sourceOrganization(sourceOrganization)
                    .messageType(messageType)
                    .patientId(patientId)
                    .globalPatientId(globalPatientId)
                    .startTime(System.currentTimeMillis())
                    .build();
        });
    }

    private String extractServiceNameFromPath(String path) {
        // Extract service name from path like /api/v1/message-router/... -> message-router-service
        if (path != null && path.startsWith("/")) {
            String[] segments = path.split("/");
            if (segments.length > 3) {
                return segments[3] + "-service";
            } else if (segments.length > 2) {
                return segments[2] + "-service";
            }
        }
        return "unknown-service";
    }
}