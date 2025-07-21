package com.hie.platform.shared.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.audit.dto.AuditRequest;
import com.hie.platform.shared.audit.dto.MessageStateRequest;
import com.hie.platform.shared.audit.model.AuditStatus;
import com.hie.platform.shared.audit.model.MessageAudit;
import com.hie.platform.shared.audit.model.MessageState;
import com.hie.platform.shared.audit.repository.MessageAuditRepository;
import com.hie.platform.shared.audit.repository.MessageStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *  Author M.Ismail
 *  Reactive Service implementation for Audit operations
 *  Date 15-July-2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditTrailServiceImpl implements AuditTrailService {

    private final MessageAuditRepository messageAuditRepository;
    private final MessageStateRepository messageStateRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> createAuditEntry(AuditRequest auditRequest) {
        // Add null checks for required fields
        if (auditRequest == null || auditRequest.getMessageId() == null) {
            log.warn("Invalid audit request: auditRequest or messageId is null");
            return Mono.empty();
        }

        return messageAuditRepository.findMaxStepSequence(auditRequest.getMessageId())
                .map(maxSeq -> maxSeq + 1)
                .switchIfEmpty(Mono.just(1)) // Use switchIfEmpty instead of defaultIfEmpty for better null handling
                .flatMap(nextSequence -> {
                    try {
                        MessageAudit audit = MessageAudit.builder()
                                .messageId(auditRequest.getMessageId())
                                .correlationId(auditRequest.getCorrelationId())
                                .serviceName(auditRequest.getServiceName())
                                .status(auditRequest.getStatus())
                                .stepName(auditRequest.getStepName())
                                .stepSequence(nextSequence)
                                .requestPayload(auditRequest.getRequestPayload())
                                .responsePayload(auditRequest.getResponsePayload())
                                .errorMessage(auditRequest.getErrorMessage())
                                .metadata(auditRequest.getMetadata())
                                .processingTimeMs(auditRequest.getProcessingTimeMs())
                                .createdAt(LocalDateTime.now())
                                .build();

                        return messageAuditRepository.save(audit);
                    } catch (Exception e) {
                        log.error("Error creating audit entity for messageId: {}", auditRequest.getMessageId(), e);
                        return Mono.error(e);
                    }
                })
                .doOnSuccess(saved -> log.debug("Created audit entry for messageId: {}, stepName: {}",
                        auditRequest.getMessageId(), auditRequest.getStepName()))
                .doOnError(error -> log.error("Error creating audit entry for messageId: {}",
                        auditRequest.getMessageId(), error))
                .onErrorResume(error -> {
                    log.warn("Audit entry creation failed, continuing without audit for messageId: {}",
                            auditRequest.getMessageId());
                    return Mono.empty();
                })
                .then();
    }

    @Override
    public Mono<Void> createMessageState(MessageStateRequest messageStateRequest) {
        if (messageStateRequest == null || messageStateRequest.getMessageId() == null) {
            log.warn("Invalid message state request: request or messageId is null");
            return Mono.empty();
        }

        try {
            MessageState messageState = MessageState.builder()
                    .messageId(messageStateRequest.getMessageId())
                    .currentStatus(messageStateRequest.getCurrentStatus())
                    .sourceOrganization(messageStateRequest.getSourceOrganization())
                    .messageType(messageStateRequest.getMessageType())
                    .patientId(messageStateRequest.getPatientId())
                    .globalPatientId(messageStateRequest.getGlobalPatientId())
                    .s3Location(messageStateRequest.getS3Location())
                    .lastProcessedBy(messageStateRequest.getLastProcessedBy())
                    .totalProcessingTimeMs(messageStateRequest.getAdditionalProcessingTime() != null ?
                            messageStateRequest.getAdditionalProcessingTime() : 0L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return messageStateRepository.save(messageState)
                    .doOnSuccess(saved -> log.debug("Created message state for messageId: {}",
                            messageStateRequest.getMessageId()))
                    .doOnError(error -> log.error("Error creating message state for messageId: {}",
                            messageStateRequest.getMessageId(), error))
                    .onErrorResume(error -> {
                        log.warn("Message state creation failed, continuing without state tracking for messageId: {}",
                                messageStateRequest.getMessageId());
                        return Mono.empty();
                    })
                    .then();
        } catch (Exception e) {
            log.error("Error building message state for messageId: {}", messageStateRequest.getMessageId(), e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> updateMessageState(MessageStateRequest messageStateRequest) {
        if (messageStateRequest == null || messageStateRequest.getMessageId() == null) {
            log.warn("Invalid message state update request: request or messageId is null");
            return Mono.empty();
        }

        Long additionalTime = messageStateRequest.getAdditionalProcessingTime() != null ?
                messageStateRequest.getAdditionalProcessingTime() : 0L;

        Mono<Integer> updateStatus = messageStateRepository.updateMessageStatus(
                messageStateRequest.getMessageId(),
                messageStateRequest.getCurrentStatus(),
                messageStateRequest.getLastProcessedBy(),
                additionalTime
        );

        Mono<Void> updateS3Location = Mono.empty();
        if (messageStateRequest.getS3Location() != null) {
            updateS3Location = messageStateRepository.findByMessageId(messageStateRequest.getMessageId())
                    .flatMap(existingState -> {
                        existingState.setS3Location(messageStateRequest.getS3Location());
                        existingState.setUpdatedAt(LocalDateTime.now());
                        return messageStateRepository.save(existingState);
                    })
                    .then();
        }

        return updateStatus
                .then(updateS3Location)
                .doOnSuccess(unused -> log.debug("Updated message state for messageId: {}",
                        messageStateRequest.getMessageId()))
                .doOnError(error -> log.error("Error updating message state for messageId: {}",
                        messageStateRequest.getMessageId(), error))
                .onErrorResume(error -> {
                    log.warn("Message state update failed for messageId: {}",
                            messageStateRequest.getMessageId());
                    return Mono.empty();
                })
                .then();
    }

    @Override
    public Mono<Void> startStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String requestPayload) {
        if (messageId == null) {
            log.warn("Cannot start step: messageId is null");
            return Mono.empty();
        }

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("timestamp", LocalDateTime.now().toString());
            metadata.put("action", "step_started");

            AuditRequest auditRequest = AuditRequest.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .serviceName(serviceName != null ? serviceName : "unknown-service")
                    .status(AuditStatus.STARTED.name())
                    .stepName(stepName != null ? stepName : "unknown-step")
                    .requestPayload(requestPayload)
                    .metadata(toJson(metadata))
                    .build();

            return createAuditEntry(auditRequest);
        } catch (Exception e) {
            log.error("Error starting audit step for messageId: {}", messageId, e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> completeStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String responsePayload, Long processingTimeMs) {
        if (messageId == null) {
            log.warn("Cannot complete step: messageId is null");
            return Mono.empty();
        }

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("timestamp", LocalDateTime.now().toString());
            metadata.put("action", "step_completed");
            metadata.put("processing_time_ms", processingTimeMs != null ? processingTimeMs : 0L);

            AuditRequest auditRequest = AuditRequest.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .serviceName(serviceName != null ? serviceName : "unknown-service")
                    .status(AuditStatus.COMPLETED.name())
                    .stepName(stepName != null ? stepName : "unknown-step")
                    .responsePayload(responsePayload)
                    .processingTimeMs(processingTimeMs != null ? processingTimeMs : 0L)
                    .metadata(toJson(metadata))
                    .build();

            return createAuditEntry(auditRequest);
        } catch (Exception e) {
            log.error("Error completing audit step for messageId: {}", messageId, e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<Void> failStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String errorMessage, Long processingTimeMs) {
        if (messageId == null) {
            log.warn("Cannot fail step: messageId is null");
            return Mono.empty();
        }

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("timestamp", LocalDateTime.now().toString());
            metadata.put("action", "step_failed");
            metadata.put("processing_time_ms", processingTimeMs != null ? processingTimeMs : 0L);

            AuditRequest auditRequest = AuditRequest.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .serviceName(serviceName != null ? serviceName : "unknown-service")
                    .status(AuditStatus.FAILED.name())
                    .stepName(stepName != null ? stepName : "unknown-step")
                    .errorMessage(errorMessage != null ? errorMessage : "Unknown error")
                    .processingTimeMs(processingTimeMs != null ? processingTimeMs : 0L)
                    .metadata(toJson(metadata))
                    .build();

            return createAuditEntry(auditRequest);
        } catch (Exception e) {
            log.error("Error recording audit step failure for messageId: {}", messageId, e);
            return Mono.empty();
        }
    }

    @Override
    public String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return "{}";
        }
    }
}