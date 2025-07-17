package com.hie.platform.gateway.filter;

import com.hie.platform.gateway.utils.AppConstant;
import com.hie.platform.shared.audit.service.AuditTrailService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author mismail
 * @description OAuthAuditGatewayFilter class handles ...
 * @since 17/07/2025
 */
@Component
@Slf4j
public class OAuthAuditGatewayFilter {
/*public class OAuthAuditGatewayFilter extends AbstractGatewayFilterFactory<OAuthAuditGatewayFilter.Config> {


    public OAuthAuditGatewayFilter() {
        super(Config.class);
        log.info("OAuthAuditGatewayFilterFactory initialized"); // Add this line
    }

    @Autowired
    private AuditTrailService auditService;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            String method = request.getMethod().name();

            // Check if this is an OAuth endpoint
            if (isOAuthEndpoint(path)) {
                String correlationId = extractOrGenerateCorrelationId(request);
                String messageId = extractOrGenerateMessageId(request);
                String stepName = determineOAuthStepName(path, method);

                // Log OAuth request started
                auditService.startStep(UUID.fromString(correlationId), UUID.fromString(messageId), stepName + "_STARTED",
                        "OAuth request started: " + method + " " + path, null);

                long startTime = System.currentTimeMillis();

                return chain.filter(exchange)
                        .doOnSuccess(aVoid -> {
                            // Log OAuth request completed
                            long duration = System.currentTimeMillis() - startTime;
                            ServerHttpResponse response = exchange.getResponse();
                            String status = response.getStatusCode().is2xxSuccessful() ? "COMPLETED" : "FAILED";

                            auditService.completeStep(UUID.fromString(correlationId), UUID.fromString(messageId), "auth-service", stepName + "_" + status,
                                    "OAuth request " + status.toLowerCase() + ": " + response.getStatusCode(),
                                    duration);
                        })
                        .doOnError(error -> {
                            // Log OAuth request failed
                            long duration = System.currentTimeMillis() - startTime;
                            *//*auditService.logAuditStep(correlationId, "auth-service", stepName + "_FAILED",
                                    "OAuth request failed: " + error.getMessage(), duration);*//*
                            auditService.failStep(UUID.fromString(correlationId), UUID.fromString(messageId), "auth-service", stepName,
                                    "OAuth request failed" + error.getMessage(),
                                    duration);
                        });
            }

            return chain.filter(exchange);
        };
    }

    private boolean isOAuthEndpoint(String path) {
        return path.startsWith("/oauth2/") ||
                path.startsWith("/oauth/") ||
                path.equals("/oauth/token") ||
                path.equals("/oauth/authorize") ||
                path.equals("/oauth/check_token") ||
                path.equals("/oauth/token_key") ||
                path.startsWith("/login/oauth2/") ||
                path.startsWith("/.well-known/");
    }

    private String determineOAuthStepName(String path, String method) {
        if (path.contains("/token")) {
            return "OAUTH_TOKEN_REQUEST";
        } else if (path.contains("/authorize")) {
            return "OAUTH_AUTHORIZE_REQUEST";
        } else if (path.contains("/userinfo")) {
            return "OAUTH_USERINFO_REQUEST";
        } else if (path.contains("/introspect")) {
            return "OAUTH_TOKEN_INTROSPECTION";
        } else if (path.contains("/revoke")) {
            return "OAUTH_TOKEN_REVOCATION";
        } else {
            return "OAUTH_REQUEST";
        }
    }

    private String extractOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(AppConstant.CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private String extractOrGenerateMessageId(ServerHttpRequest request) {
        String id = request.getHeaders().getFirst(AppConstant.MESSAGE_ID_HEADER);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    @Override
    public String name() {
        return "OAuthAuditFilter";
    }

    public static class Config {
        private String serviceName;

        public Config(){

        }

        public Config(String serviceName){
            this.serviceName=serviceName;
        }
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
    }*/
}
