package com.hie.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        log.info("GatewayAuditFilter initialized and registered");
    }

    @Override
    public GatewayFilter apply(Config config) {
        log.info("GatewayAuditFilter.apply() called with config: {}", config);

        return (exchange, chain) -> {
            log.info("GatewayAuditFilter executing for request: {} {}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI());

            // Add a flag to prevent double execution
            String executionKey = config.getServiceName(); //"audit-filter-executed";
            if (exchange.getAttributes().containsKey(executionKey)) {
                log.debug("Filter already executed, skipping...");
                return chain.filter(exchange);
            }

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

            // Generate or propagate message ID
            String messageId = exchange.getRequest().getHeaders().getFirst(MESSAGE_ID_HEADER);
            if (messageId == null) {
                messageId = UUID.randomUUID().toString();
                requestBuilder.header(MESSAGE_ID_HEADER, messageId);
                log.debug("Generated new message ID: {}", messageId);
            } else {
                log.debug("Propagating existing message ID: {}", messageId);
            }

            // Generate or propagate correlation ID
            String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
                requestBuilder.header(CORRELATION_ID_HEADER, correlationId);
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Propagating existing correlation ID: {}", correlationId);
            }

            // Add service name if configured
            if (config.getServiceName() != null) {
                requestBuilder.header(SERVICE_NAME_HEADER, config.getServiceName());
                log.debug("Added service name header: {}", config.getServiceName());
            }

            // Propagate audit headers if they exist
            propagateHeaderIfExists(exchange, requestBuilder, SOURCE_ORGANIZATION_HEADER);
            propagateHeaderIfExists(exchange, requestBuilder, MESSAGE_TYPE_HEADER);
            propagateHeaderIfExists(exchange, requestBuilder, PATIENT_ID_HEADER);
            propagateHeaderIfExists(exchange, requestBuilder, GLOBAL_PATIENT_ID_HEADER);

            log.info("Gateway audit filter applied - MessageId: {}, CorrelationId: {}, Service: {}",
                    messageId, correlationId, config.getServiceName());

            exchange.getAttributes().put(executionKey, true);

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        };
    }

    private void propagateHeaderIfExists(org.springframework.web.server.ServerWebExchange exchange,
                                         ServerHttpRequest.Builder requestBuilder, String headerName) {
        String headerValue = exchange.getRequest().getHeaders().getFirst(headerName);
        if (headerValue != null) {
            requestBuilder.header(headerName, headerValue);
            log.debug("Propagated header {}: {}", headerName, headerValue);
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

        @Override
        public String toString() {
            return "Config{serviceName='" + serviceName + "'}";
        }
    }
}