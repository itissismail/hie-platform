package com.hie.platform.shared.service;

import com.hie.platform.shared.audit.context.AuditContext;
import com.hie.platform.shared.dto.AuditRequest;
import com.hie.platform.shared.model.AuditStatus;
import com.hie.platform.shared.service.audit.AuditTrailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class CustomAuditService {

    @Autowired
    private AuditTrailService auditTrailService;

    public Mono<Void> auditCustomStep(UUID messageId, String stepName, Object request, Object response, long startTime) {
        AuditRequest auditRequest = AuditRequest.builder()
                .messageId(messageId)
                .correlationId(AuditContext.getCorrelationId())
                .serviceName("custom-service")
                .stepName(stepName)
                .status(AuditStatus.COMPLETED.name())
                .requestPayload(auditTrailService.toJson(request))
                .responsePayload(auditTrailService.toJson(response))
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();

        return auditTrailService.createAuditEntry(auditRequest);
    }
}
