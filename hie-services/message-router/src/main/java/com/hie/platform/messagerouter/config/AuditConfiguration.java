package com.hie.platform.messagerouter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditConfiguration {

 /*   @Bean
    @ConditionalOnMissingBean
    public AuditTrailService auditTrailService(MessageAuditRepository messageAuditRepository, MessageStateRepository messageStateRepository, ObjectMapper objectMapper) {
        return new AuditTrailServiceImpl(messageAuditRepository, messageStateRepository, objectMapper);

    }*/
}