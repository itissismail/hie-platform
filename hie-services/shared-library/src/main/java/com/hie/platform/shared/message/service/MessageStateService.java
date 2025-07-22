package com.hie.platform.shared.message.service;

import com.hie.platform.shared.audit.model.MessageState;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.message.repository.MessageStateRepository;
import com.hie.platform.shared.minio.model.FileUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author mismail
 * @description MessageStateService class handles ...
 * @since 22/07/2025
 */
@Service
@Slf4j
public class MessageStateService {
    private final MessageStateRepository messageStateRepository;

    @Autowired
    public MessageStateService(MessageStateRepository messageStateRepository) {
        this.messageStateRepository = messageStateRepository;
    }

    /**
     * Update message state after successful publishing (REACTIVE)
     */
    public Mono<MessageState> updateMessageStateAfterPublish(UUID messageId, String correlationId, String serviceName) {
        log.debug("Updating message state after successful publish for messageId: {}", messageId);

        return messageStateRepository.findByMessageId(messageId)
                .flatMap(messageState -> {
                    log.debug("Existing message state found, updating...");

                    // Update existing fields
                    messageState.setCurrentStatus(MessageStatus.QUEUED.name());
                    messageState.setUpdatedAt(LocalDateTime.now());
                    messageState.setLastProcessedBy(serviceName);

                    return messageStateRepository.save(messageState);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("MessageState not found for messageId: " + messageId)));

    }


    public Mono<MessageState> createMessageStateEntry(UUID messageId, UUID correlationId,
                                                       String messageType, String patientId,
                                                       String sourceOrganization, FileUploadResult uploadResult, String serviceName) {

        log.debug("Creating or updating message state entry for messageId: {}", messageId);

        return messageStateRepository.findByMessageId(messageId)
                .flatMap(existing -> {
                    log.debug("Existing message state found, updating...");

                    // Update existing fields
                    existing.setCurrentStatus(MessageStatus.PROCESSING.name());
                    existing.setSourceOrganization(sourceOrganization);
                    existing.setMessageType(messageType);
                    existing.setPatientId(patientId);
                    existing.setS3Location(uploadResult.getS3Location());
                    existing.setLastProcessedBy(serviceName);
                    existing.setTotalProcessingTimeMs(0L);
                    existing.setUpdatedAt(LocalDateTime.now());

                    return messageStateRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("No existing state found, inserting new message state entry...");

                    MessageState newState = MessageState.builder()
                            .messageId(messageId)
                            .correlationId(correlationId)
                            .currentStatus(MessageStatus.PROCESSING.name())
                            .sourceOrganization(sourceOrganization)
                            .messageType(messageType)
                            .patientId(patientId)
                            .s3Location(uploadResult.getS3Location())
                            .lastProcessedBy(serviceName)
                            .totalProcessingTimeMs(0L)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return messageStateRepository.save(newState);
                }));
    }

    /**
     * Create failed message state entry (REACTIVE)
     */
    public Mono<MessageState> createFailedMessageStateEntry(UUID messageId, UUID correlationId,
                                                             String messageType, String patientId,
                                                             String sourceOrganization, String errorMessage, String serviceName) {

        log.debug("Creating failed message state entry for messageId: {}", messageId);


        return messageStateRepository.findByMessageId(messageId)
                .flatMap(existing -> {
                    log.debug("Existing message state found while adding failed message state entry , updating...");

                    // Update existing fields
                    existing.setCurrentStatus(MessageStatus.FAILED.name());
                    existing.setSourceOrganization(sourceOrganization);
                    existing.setMessageType(messageType);
                    existing.setPatientId(patientId);
                    existing.setLastProcessedBy(serviceName);
                    existing.setTotalProcessingTimeMs(0L);
                    existing.setErrorMessage(errorMessage);
                    existing.setCreatedAt(LocalDateTime.now());
                    existing.setUpdatedAt(LocalDateTime.now());

                    log.debug("Updating existing  messageState with id: {}", existing.getId());
                    return messageStateRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("No existing state found, inserting new message state entry...");

                    MessageState messageState = MessageState.builder()
                            .messageId(messageId)
                            .correlationId(correlationId)
                            .currentStatus(MessageStatus.FAILED.name())
                            .sourceOrganization(sourceOrganization)
                            .messageType(messageType)
                            .patientId(patientId)
                            .lastProcessedBy(serviceName)
                            .totalProcessingTimeMs(0L)
                            .errorMessage(errorMessage)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    log.debug("Saving a new messageState with id: {}", messageState.getId());
                    return messageStateRepository.save(messageState);
                }));
    }

}
