package com.hie.platform.messagerouter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.repository.MessageAuditRepository;
import com.hie.platform.shared.repository.MessageStateRepository;
import com.hie.platform.shared.service.audit.AuditTrailService;
import com.hie.platform.shared.service.audit.AuditTrailServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditTrailService auditTrailService(MessageAuditRepository messageAuditRepository, MessageStateRepository messageStateRepository, ObjectMapper objectMapper) {
        return new AuditTrailServiceImpl(messageAuditRepository, messageStateRepository, objectMapper);

    }
}