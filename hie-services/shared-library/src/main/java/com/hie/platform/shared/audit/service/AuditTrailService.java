package com.hie.platform.shared.audit.service;

import com.hie.platform.shared.audit.dto.AuditRequest;
import com.hie.platform.shared.audit.dto.MessageStateRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;


/**
 *  Author M.Ismail
 *  Service interface for Audit operation
 *  Date 15-July-2025
 */
public interface AuditTrailService {

    Mono<Void> createAuditEntry(AuditRequest auditRequest);

    Mono<Void> createMessageState(MessageStateRequest messageStateRequest);

    Mono<Void> updateMessageState(MessageStateRequest messageStateRequest);

    Mono<Void> startStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String requestPayload);

    Mono<Void> completeStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String responsePayload, Long processingTimeMs);

    Mono<Void> failStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String errorMessage, Long processingTimeMs);
    String toJson(Object obj) ;
}