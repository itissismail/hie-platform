package com.hie.platform.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/*@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})*/
@SpringBootApplication
@ComponentScan(basePackages = {"com.hie.platform.gateway", "com.hie.platform.shared"})
//@EnableJpaRepositories(basePackages = "com.hie.platform.shared.audit.repository")
@EnableR2dbcRepositories(basePackages = "com.hie.platform.shared.audit.repository")
@EntityScan(basePackages = "com.hie.platform.shared")
//        ,excludeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {com.hie.platform.shared.service.AuditService.class}
//        ))
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

/*    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("message-router", r -> r.path("/api/v1/messages/**")
                        .uri("lb://message-router-service"))
                .route("validation-service", r -> r.path("/api/v1/validation/**")
                        .uri("lb://validation-service"))
                .route("intake-service", r -> r.path("/api/v1/intake/**")
                        .uri("lb://intake-service"))
                .route("conversion-service", r -> r.path("/api/v1/conversion/**")
                        .uri("lb://conversion-service"))
                .route("storage-service", r -> r.path("/api/v1/storage/**")
                        .uri("lb://storage-service"))
                .build();

    }*/
}

// Security Configuration
/*
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
     */
/*   return http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/v1/messages/**").hasRole("HIE_CLIENT")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder())))
                .build();*//*

        return null;
    }

   */
/* @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
                .build();
    }*//*

}

// Rate Limiting Configuration
@Component
class RateLimitConfiguration {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
   */
/*     return exchange -> exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> jwt.getClaimAsString("sub"))
                .switchIfEmpty(Mono.just("anonymous"));*//*

        return null;
    }
}

// Global Filter for Request Logging
@Component
class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        logger.info("Request: {} {} from {}",
                request.getMethod(),
                request.getURI(),
                request.getRemoteAddress());

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    ServerHttpResponse response = exchange.getResponse();
                    logger.info("Response: {} for {} {}",
                            response.getStatusCode(),
                            request.getMethod(),
                            request.getURI());
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
*/
