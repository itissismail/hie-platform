package com.hie.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class GatewayAuditFilter extends AbstractGatewayFilterFactory<GatewayAuditFilter.Config> {

    private static final String MESSAGE_ID_HEADER = "X-Message-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SOURCE_ORGANIZATION_HEADER = "X-Source-Organization";
    private static final String MESSAGE_TYPE_HEADER = "X-Message-Type";
    private static final String PATIENT_ID_HEADER = "X-Patient-ID";
    private static final String GLOBAL_PATIENT_ID_HEADER = "X-Global-Patient-ID";

    public GatewayAuditFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

            // Generate or propagate message ID
            String messageId = exchange.getRequest().getHeaders().getFirst(MESSAGE_ID_HEADER);
            if (messageId == null) {
                messageId = UUID.randomUUID().toString();
                requestBuilder.header(MESSAGE_ID_HEADER, messageId);
            }

            // Generate or propagate correlation ID
            String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
                requestBuilder.header(CORRELATION_ID_HEADER, correlationId);
            }

            // Add service name if configured
            if (config.getServiceName() != null) {
                requestBuilder.header(SERVICE_NAME_HEADER, config.getServiceName());
            }

            // Propagate audit headers if they exist
            propagateHeaderIfExists(exchange, requestBuilder, SOURCE_ORGANIZATION_HEADER);
            propagateHeaderIfExists(exchange, requestBuilder, MESSAGE_TYPE_HEADER);
            propagateHeaderIfExists(exchange, requestBuilder, PATIENT_ID_HEADER);
            propagateHeaderIfExists(exchange, requestBuilder, GLOBAL_PATIENT_ID_HEADER);

            log.debug("Gateway audit filter applied - MessageId: {}, CorrelationId: {}, Service: {}",
                    messageId, correlationId, config.getServiceName());

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        };
    }

    private void propagateHeaderIfExists(org.springframework.web.server.ServerWebExchange exchange,
                                         ServerHttpRequest.Builder requestBuilder, String headerName) {
        String headerValue = exchange.getRequest().getHeaders().getFirst(headerName);
        if (headerValue != null) {
            requestBuilder.header(headerName, headerValue);
        }
    }

    public static class Config {
        private String serviceName;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }
    }
}