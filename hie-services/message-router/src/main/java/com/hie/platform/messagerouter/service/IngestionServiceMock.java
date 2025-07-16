package com.hie.platform.messagerouter.service;

import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.service.AuditTrailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
    public class IngestionServiceMock {

    @Autowired
    private AuditTrailService auditTrailService;
    @AuditStep(serviceName = "message-router", stepName = "hl7-parsing")
    public String ingest(String hl7Message) {
            // Upload to MinIO, log audit, publish to RabbitMQ, etc. (non-blocking if possible)
            return UUID.randomUUID().toString();

            // Manual Update
     /*   MessageStateRequest request = MessageStateRequest.builder()
                .messageId(UUID.randomUUID())
                .currentStatus("hl7-parsing")
                .lastProcessedBy("message-router")
                .additionalProcessingTime(1000L)
                .build();

         return auditTrailService.updateMessageState(request);*/
        }
    }
