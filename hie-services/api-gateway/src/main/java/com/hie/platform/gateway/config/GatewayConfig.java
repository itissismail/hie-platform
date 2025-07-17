package com.hie.platform.gateway.config;

import com.hie.platform.gateway.filter.GatewayAuditFilter;
import com.hie.platform.gateway.filter.OAuthAuditGatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;

//@Configuration
public class GatewayConfig {

    // Use application.yaml declarative style for defining routes
   /* @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           GatewayAuditFilter gatewayAuditFilter,
                                           OAuthAuditGatewayFilter oauthAuditFilter) {
        return builder.routes()
                // Message service route
                .route("message-router-ingest", r -> r
                        .path("/intake/**")
                        .filters(f -> f.filter(gatewayAuditFilter.apply(new GatewayAuditFilter.Config("message-router-service"))))
                        .uri("http://localhost:8081"))

                // Auth service route with OAuth auditing
                .route("auth-service", r -> r
                        .path("/oauth/**", "/oauth2/**", "/login/**", "/.well-known/**")
                        .filters(f -> f.filter(oauthAuditFilter.apply(new OAuthAuditGatewayFilter.Config("oAuth-Service"))))
                        .uri("http://localhost:8082"))

                .build();
    }


    @Bean
    public GatewayFilter oAuthAuditFilter() {
        return (exchange, chain) -> {
            // Your OAuth audit logic here
            ServerHttpRequest request = exchange.getRequest();
            System.out.println("OAuth Audit - Path: " + request.getPath());
            return chain.filter(exchange);
        };
    }*/
}
