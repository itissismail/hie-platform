package com.hie.platform.shared.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.audit.aspect.NonReactiveAuditTrailAspect;
import com.hie.platform.shared.audit.service.NonReactiveAuditTrailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 *  Author M.Ismail
 *  Reactive Spring Boot auto-configuration class for auditing functionality
 *  Date 15-July-2025
 */

@Configuration
@EnableAsync
@EnableR2dbcAuditing
//@EnableR2dbcRepositories(basePackages = "com.hie.platform.shared.audit.repository")
@EnableR2dbcRepositories(basePackages = {
        "com.hie.platform.shared.message.repository",
        "com.hie.platform.shared.audit.repository"
})
@ComponentScan(basePackages = "com.hie.platform.shared")
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(name = "hie.audit.database.enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public NonReactiveAuditTrailService nonReactiveAuditTrailService(
            com.hie.platform.shared.audit.repository.MessageAuditRepository messageAuditRepository) {
        return new NonReactiveAuditTrailService(messageAuditRepository);
    }


    @Bean
    @ConditionalOnMissingBean
    public NonReactiveAuditTrailAspect nonReactiveAuditTrailAspect(
            NonReactiveAuditTrailService nonReactiveAuditTrailService) {
        return new NonReactiveAuditTrailAspect(nonReactiveAuditTrailService);
    }
}