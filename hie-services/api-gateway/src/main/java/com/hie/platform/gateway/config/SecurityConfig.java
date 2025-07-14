package com.hie.platform.gateway.config;

import com.hie.platform.gateway.config.roles.AclLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

import java.util.List;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

    /*
    * Authentication Flow: **Used by default**
    * Request arrives → Spring Security filter chain intercepts
    * Check path → If /health, allow; otherwise require auth
    * Basic Auth → Spring Security validates against UserDetailsService
    * UserContextFilter → Adds user headers after Spring Security authentication
    * Route → Forward to message router
    * */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
      /*  return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/health").permitAll()
                        .pathMatchers("/api/clients/**").permitAll()
                        .pathMatchers("/oauth2/**").permitAll()
                        .anyExchange().authenticated()
                )
                //.httpBasic(Customizer.withDefaults())
                .httpBasic().disable()
                .csrf().disable()
                .build();*/
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/health").permitAll()
                        .pathMatchers("/api/clients/**", "/oauth2/**").permitAll()
                        //.pathMatchers("/ingest/**").hasAuthority("SCOPE_ingest") // or just `.authenticated()`
                        //.pathMatchers("/ingest/**").access(this::authorizeIngest)
                        .pathMatchers("/ingest/**").access(this::authorizeViaAcl)
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())) // ✅ Enable JWT

                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .authorities("ROLE_ADMIN")
                .build();

        return new MapReactiveUserDetailsService(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private Mono<AuthorizationDecision> authorizeIngest(Mono<Authentication> auth, AuthorizationContext context) {
        return auth.map(authentication -> {
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                List<String> roles = jwtAuth.getToken().getClaimAsStringList("roles");
                return new AuthorizationDecision(roles != null && roles.contains("write"));
            } else if (authentication.isAuthenticated()) {
                // Basic Auth fallback
                return new AuthorizationDecision(true);
            }
            return new AuthorizationDecision(false);
        });
    }


    @Autowired
    private AclLoader aclLoader;

    private Mono<AuthorizationDecision> authorizeViaAcl(Mono<Authentication> authMono, AuthorizationContext context) {
        String path = context.getExchange().getRequest().getPath().value();
        //String method = context.getExchange().getRequest().getMethodValue();
        HttpMethod httpMethod = context.getExchange().getRequest().getMethod();
        String method = (httpMethod != null) ? httpMethod.name() : "UNKNOWN";

        return authMono.map(auth -> {
            if (auth instanceof JwtAuthenticationToken jwt) {
                List<String> roles = jwt.getToken().getClaimAsStringList("roles");
                boolean allowed = roles.stream().anyMatch(role -> aclLoader.isAllowed(role, path, method));
                return new AuthorizationDecision(allowed);
            }
            return new AuthorizationDecision(false);
        });
    }
}
