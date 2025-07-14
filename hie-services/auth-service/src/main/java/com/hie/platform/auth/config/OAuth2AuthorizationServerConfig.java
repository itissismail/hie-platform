package com.hie.platform.auth.config;


import com.hie.platform.auth.entity.Client;
import com.hie.platform.auth.service.ClientService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth2 Authorization Server Configuration
 * Configures Spring Authorization Server for client credentials flow with JWT tokens
 */
@Configuration
@EnableWebSecurity
public class OAuth2AuthorizationServerConfig {

    private final ClientService clientService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600}")
    private int jwtExpiration;

    @Autowired
    public OAuth2AuthorizationServerConfig(@Lazy ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Authorization Server Security Filter Chain
     * Configures the OAuth2 authorization server endpoints
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0

        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
        );

        return http.build();
    }

    /**
     * Default Security Filter Chain
     * Configures security for non-authorization server endpoints
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/clients/**").permitAll()
                        .requestMatchers("/oauth2/token").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults());
                //.formLogin(Customizer.withDefaults());


        return http.build();
    }

    /**
     * Registered Client Repository
     * Provides client details for OAuth2 authorization
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new CustomRegisteredClientRepository(clientService);
    }

    /**
     * JWT Decoder for token validation
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * JWK Source for JWT signing
     * Supports both symmetric and asymmetric signatures
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Generate RSA Key Pair for JWT signing
     */
    private KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Authorization Server Settings
     * Configures authorization server endpoints
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8082")
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .jwkSetEndpoint("/oauth2/jwks")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .build();
    }

    /**
     * Password Encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * JWT Token Customizer
     * Adds custom claims to JWT tokens including client roles
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                String clientId = context.getRegisteredClient().getClientId();

                // Get client from database to fetch roles
                Optional<Client> clientOpt = clientService.findByClientId(clientId);
                if (clientOpt.isPresent()) {
                    Client client = clientOpt.get();

                    // Add custom claims
                    context.getClaims()
                            .claim("client_id", client.getClientId())
                            .claim("client_name", client.getClientName())
                            .claim("roles", client.getRolesList())
                            .claim("scopes", client.getScopes().split(","))
                            .claim("grant_type", "client_credentials")
                            .claim("custom-claim","custom-value");
                }
            }
        };
    }

    /**
     * Custom Registered Client Repository Implementation
     * Integrates with our custom Client entity
     */
    public static class CustomRegisteredClientRepository implements RegisteredClientRepository {

        private final ClientService clientService;

        public CustomRegisteredClientRepository(ClientService clientService) {
            this.clientService = clientService;
        }

        @Override
        public void save(RegisteredClient registeredClient) {
            // Not implemented - clients are managed through ClientService
            throw new UnsupportedOperationException("Use ClientService for client management");
        }

        @Override
        public RegisteredClient findById(String id) {
            return clientService.findByClientId(id)
                    .map(this::mapToRegisteredClient)
                    .orElse(null);
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return clientService.findByClientId(clientId)
                    .map(this::mapToRegisteredClient)
                    .orElse(null);
        }

        /**
         * Map our Client entity to Spring Security's RegisteredClient
         */
        private RegisteredClient mapToRegisteredClient(Client client) {
            return RegisteredClient.withId(client.getId().toString())
                    .clientId(client.getClientId())
                    .clientSecret(client.getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scopes(scopeSet -> {
                        String[] scopes = client.getScopes().split(",");
                        for (String scope : scopes) {
                            scopeSet.add(scope.trim());
                        }
                    })
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTokenValidity()))
                            .build())
                    .clientSettings(ClientSettings.builder()
                            .requireAuthorizationConsent(false)
                            .build())
                    .build();
        }
    }
}