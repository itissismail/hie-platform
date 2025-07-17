package com.hie.platform.messagerouter.service;

import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.audit.service.AuditTrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class IngestionServiceMock {

    @Autowired
    private AuditTrailService auditTrailService;

    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.PROCESSING)
    public String ingest(String hl7Message) {
        log.debug("IngestionServiceMock.ingest() called");
        log.debug("Processing HL7 message: {}",
                hl7Message != null ? hl7Message.substring(0, Math.min(100, hl7Message.length())) + "..." : "null");

        // Upload to MinIO, log audit, publish to RabbitMQ, etc. (non-blocking if possible)
        String correlationId = UUID.randomUUID().toString();

        log.debug("Generated correlation ID: {}", correlationId);
        return correlationId;
    }
}