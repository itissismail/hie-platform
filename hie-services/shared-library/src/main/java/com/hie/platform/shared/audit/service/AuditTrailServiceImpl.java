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
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 *  Author M.Ismail
 *  Service interface for Audit operation
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
        return Mono.fromCallable(() -> {
                    Integer nextSequence = Optional.ofNullable(
                            messageAuditRepository.findMaxStepSequence(auditRequest.getMessageId())
                    ).map(seq -> seq + 1).orElse(1);

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
                            .build();

                    messageAuditRepository.save(audit);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> log.error("Error creating audit entry for messageId: {}", auditRequest.getMessageId(), error))
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> createMessageState(MessageStateRequest messageStateRequest) {
        return Mono.fromCallable(() -> {
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
                            .build();

                    messageStateRepository.save(messageState);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> log.error("Error creating message state for messageId: {}", messageStateRequest.getMessageId(), error))
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    @Override
    @Transactional
    public Mono<Void> updateMessageState(MessageStateRequest messageStateRequest) {
        return Mono.fromCallable(() -> {
                    messageStateRepository.updateMessageStatus(
                            messageStateRequest.getMessageId(),
                            messageStateRequest.getCurrentStatus(),
                            messageStateRequest.getLastProcessedBy(),
                            messageStateRequest.getAdditionalProcessingTime() != null ?
                                    messageStateRequest.getAdditionalProcessingTime() : 0L
                    );

                    // Update S3 location if provided
                    if (messageStateRequest.getS3Location() != null) {
                        Optional<MessageState> existingState = messageStateRepository.findByMessageId(messageStateRequest.getMessageId());
                        if (existingState.isPresent()) {
                            MessageState state = existingState.get();
                            state.setS3Location(messageStateRequest.getS3Location());
                            messageStateRepository.save(state);
                        }
                    }

                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(error -> log.error("Error updating message state for messageId: {}", messageStateRequest.getMessageId(), error))
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    @Override
    public Mono<Void> startStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String requestPayload) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", LocalDateTime.now().toString());
        metadata.put("action", "step_started");

        AuditRequest auditRequest = AuditRequest.builder()
                .messageId(messageId)
                .correlationId(correlationId)
                .serviceName(serviceName)
                .status(AuditStatus.STARTED.name())
                .stepName(stepName)
                .requestPayload(requestPayload)
                .metadata(toJson(metadata))
                .build();

        return createAuditEntry(auditRequest);
    }

    @Override
    public Mono<Void> completeStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String responsePayload, Long processingTimeMs) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", LocalDateTime.now().toString());
        metadata.put("action", "step_completed");

        AuditRequest auditRequest = AuditRequest.builder()
                .messageId(messageId)
                .correlationId(correlationId)
                .serviceName(serviceName)
                .status(AuditStatus.COMPLETED.name())
                .stepName(stepName)
                .responsePayload(responsePayload)
                .processingTimeMs(processingTimeMs)
                .metadata(toJson(metadata))
                .build();

        return createAuditEntry(auditRequest);
    }

    @Override
    public Mono<Void> failStep(UUID messageId, UUID correlationId, String serviceName, String stepName, String errorMessage, Long processingTimeMs) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("timestamp", LocalDateTime.now().toString());
        metadata.put("action", "step_failed");

        AuditRequest auditRequest = AuditRequest.builder()
                .messageId(messageId)
                .correlationId(correlationId)
                .serviceName(serviceName)
                .status(AuditStatus.FAILED.name())
                .stepName(stepName)
                .errorMessage(errorMessage)
                .processingTimeMs(processingTimeMs)
                .metadata(toJson(metadata))
                .build();

        return createAuditEntry(auditRequest);
    }

    @Override
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON", e);
            return "{}";
        }
    }
}
