package com.hie.platform.shared.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 *  Author M.Ismail
 *  This is a Spring Boot auto-configuration class. It defines the default beans for auditing functionality (e.g., AuditTrailService)
 *  and registers them only if missing.
 *  Date 15-July-2025
 */

@Configuration
@EnableAsync
@EnableTransactionManagement
@EntityScan(basePackages = "com.hie.platform.shared.model")
@EnableJpaRepositories(basePackages = "com.hie.platform.shared.audit.repository")
@ComponentScan(basePackages = "com.hie.platform.shared")
//@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)

public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}