package com.hie.platform.shared.service;

import com.hie.platform.shared.model.MessageAudit;
import com.hie.platform.shared.model.MessageStatus;
import com.hie.platform.shared.repository.MessageAuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuditService {

    @Autowired
    private MessageAuditRepository auditRepository;

    public void logMessageEvent(UUID messageId, UUID correlationId, String serviceName,
                                MessageStatus status, Long processingTime, String errorMessage) {
        MessageAudit audit = new MessageAudit(messageId, correlationId, serviceName, status);
        audit.setProcessingTimeMs(processingTime);
        audit.setErrorMessage(errorMessage);

        auditRepository.save(audit);
    }

    public void logSuccess(UUID messageId, UUID correlationId, String serviceName, Long processingTime) {
        logMessageEvent(messageId, correlationId, serviceName, MessageStatus.COMPLETED, processingTime, null);
    }

    public void logFailure(UUID messageId, UUID correlationId, String serviceName, String errorMessage) {
        logMessageEvent(messageId, correlationId, serviceName, MessageStatus.FAILED, null, errorMessage);
    }
}