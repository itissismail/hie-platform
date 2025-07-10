package com.hie.platform.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    // Use application.yaml declarative style for defining routes
 /*   @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route all requests to message router
                .route("message-router", r -> r
                        .path("/**")
                        .uri("http://localhost:8081") // Message router service URL
                )
                .build();
    }*/
}
