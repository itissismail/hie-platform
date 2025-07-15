package com.hie.platform.shared.audit.filter;

import com.hie.platform.shared.audit.context.AuditContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuditContextFilter implements WebFilter {

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
                .flatMap(context -> {
                    AuditContext.setContext(context);
                    log.debug("Audit context set for messageId: {}, correlationId: {}, service: {}",
                            context.getMessageId(), context.getCorrelationId(), context.getServiceName());

                    return chain.filter(exchange)
                            .contextWrite(ctx -> ctx.put(AuditContext.class, context));
                })
                .doFinally(signalType -> {
                    log.debug("Clearing audit context");
                    AuditContext.clear();
                });
    }

    private Mono<AuditContext> extractAuditContext(ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            String messageIdStr = exchange.getRequest().getHeaders().getFirst(MESSAGE_ID_HEADER);
            String correlationIdStr = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            String serviceName = exchange.getRequest().getHeaders().getFirst(SERVICE_NAME_HEADER);
            String sourceOrganization = exchange.getRequest().getHeaders().getFirst(SOURCE_ORGANIZATION_HEADER);
            String messageType = exchange.getRequest().getHeaders().getFirst(MESSAGE_TYPE_HEADER);
            String patientId = exchange.getRequest().getHeaders().getFirst(PATIENT_ID_HEADER);
            String globalPatientId = exchange.getRequest().getHeaders().getFirst(GLOBAL_PATIENT_ID_HEADER);

            UUID messageId = messageIdStr != null ? UUID.fromString(messageIdStr) : UUID.randomUUID();
            UUID correlationId = correlationIdStr != null ? UUID.fromString(correlationIdStr) : UUID.randomUUID();

            return AuditContext.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .serviceName(serviceName != null ? serviceName : "unknown-service")
                    .sourceOrganization(sourceOrganization)
                    .messageType(messageType)
                    .patientId(patientId)
                    .globalPatientId(globalPatientId)
                    .startTime(System.currentTimeMillis())
                    .build();
        });
    }
}