package com.hie.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;

// Custom Authentication Filter (Optional - for more control)
@Component
public class BasicAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<BasicAuthGatewayFilterFactory.Config> {

    public BasicAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for health endpoint
            if (request.getPath().value().equals("/health")) {
                return chain.filter(exchange);
            }

            // Check for Authorization header
            String authHeader = request.getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                return handleUnauthorized(exchange);
            }

            // Decode Basic Auth
            String base64Credentials = authHeader.substring("Basic ".length());
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));
            String[] parts = credentials.split(":", 2);

            if (parts.length != 2) {
                return handleUnauthorized(exchange);
            }

            String username = parts[0];
            String password = parts[1];

            // Validate credentials
            if (!"admin".equals(username) || !"admin123".equals(password)) {
                return handleUnauthorized(exchange);
            }

            // Add user context to request headers for downstream services
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", username)
                    .header("X-User-Role", "ADMIN")
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);
        };
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("WWW-Authenticate", "Basic realm=\"HIE API Gateway\"");

        String body = "{\"error\":\"Unauthorized\",\"message\":\"Invalid credentials\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());

        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties if needed
    }
}