package com.hie.platform.shared.audit.service;

import com.hie.platform.shared.audit.model.MessageAudit;
import com.hie.platform.shared.audit.repository.MessageAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Author M.Ismail
 * Non-reactive audit trail service for message-based services
 * Date 28-July-2025
 *
 * UPDATED: Enhanced to properly handle messageId relationships and provide both async and sync methods
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NonReactiveAuditTrailService {

    private final MessageAuditRepository messageAuditRepository;

    /**
     * Start audit step - Async version
     * Creates audit record with new messageId while linking to previous message via previousMessageId
     *
     * @param correlationId Original correlation ID to maintain throughout the flow
     * @param previousMessageId Previous message ID from upstream service (can be null for first service)
     * @param serviceName Current service name
     * @param stepName Step being executed
     * @param requestPayload Optional request payload
     * @return New message ID for this service's processing
     */
    @Async
    public CompletableFuture<UUID> startStep(UUID correlationId, UUID previousMessageId,
                                             String serviceName, String stepName,
                                             String requestPayload) {
        UUID newMessageId = UUID.randomUUID();

        log.debug("Starting audit step: {} for service: {}, newMessageId: {}, correlationId: {}, previousMessageId: {}",
                stepName, serviceName, newMessageId, correlationId, previousMessageId);

        try {
            MessageAudit audit = MessageAudit.builder()
                    .messageId(newMessageId)
                    .correlationId(correlationId)
                    .previousMessageId(previousMessageId)
                    .serviceName(serviceName)
                    .stepName(stepName)
                    .status("IN_PROGRESS")
                    .requestPayload(truncatePayload(requestPayload))
                    .createdAt(LocalDateTime.now())
                    .stepSequence(calculateStepSequence(correlationId))
                    .build();

            messageAuditRepository.save(audit)
                    .doOnSuccess(savedAudit -> log.debug("Successfully started audit step: {} with messageId: {}",
                            stepName, newMessageId))
                    .doOnError(error -> log.error("Failed to start audit step: {} with messageId: {}",
                            stepName, newMessageId, error))
                    .subscribe();

            return CompletableFuture.completedFuture(newMessageId);

        } catch (Exception e) {
            log.error("Error starting audit step: {} for messageId: {}", stepName, newMessageId, e);
            return CompletableFuture.completedFuture(newMessageId); // Return ID even if audit fails
        }
    }

    /**
     * Start audit step - Synchronous version
     * Used by aspect when immediate processing is needed
     *
     * @param correlationId Correlation ID for the entire flow
     * @param previousMessageId Previous message ID from upstream service
     * @param serviceName Current service name
     * @param stepName Step being executed
     * @param requestPayload Optional request payload
     * @return New message ID for this service's processing
     */
    public UUID startStepSync(UUID correlationId, UUID previousMessageId,
                              String serviceName, String stepName, String requestPayload) {
        UUID newMessageId = UUID.randomUUID();

        log.debug("Starting audit step synchronously: {} for service: {}, newMessageId: {}, correlationId: {}, previousMessageId: {}",
                stepName, serviceName, newMessageId, correlationId, previousMessageId);

        try {
            MessageAudit audit = MessageAudit.builder()
                    .messageId(newMessageId)
                    .correlationId(correlationId)
                    .previousMessageId(previousMessageId)
                    .serviceName(serviceName)
                    .stepName(stepName)
                    .status("IN_PROGRESS")
                    .requestPayload(truncatePayload(requestPayload))
                    .createdAt(LocalDateTime.now())
                    .stepSequence(calculateStepSequence(correlationId))
                    .build();

            messageAuditRepository.save(audit)
                    .doOnSuccess(savedAudit -> log.debug("Successfully started audit step synchronously: {} with messageId: {}",
                            stepName, newMessageId))
                    .doOnError(error -> log.error("Failed to start audit step synchronously: {} with messageId: {}",
                            stepName, newMessageId, error))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error starting audit step synchronously: {} for messageId: {}", stepName, newMessageId, e);
        }

        return newMessageId;
    }

    /**
     * Start audit step with existing messageId - Synchronous version
     * Used when messageId is already generated (like in ValidationConsumer)
     */
    public void startStepWithExistingMessageId(UUID messageId, UUID correlationId, UUID previousMessageId,
                                               String serviceName, String stepName, String requestPayload) {
        log.debug("Starting audit step with existing messageId: {} for service: {}, correlationId: {}, previousMessageId: {}",
                messageId, serviceName, correlationId, previousMessageId);

        try {
            MessageAudit audit = MessageAudit.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .previousMessageId(previousMessageId)
                    .serviceName(serviceName)
                    .stepName(stepName)
                    .status("IN_PROGRESS")
                    .requestPayload(truncatePayload(requestPayload))
                    .createdAt(LocalDateTime.now())
                    .stepSequence(calculateStepSequence(correlationId))
                    .build();

            messageAuditRepository.save(audit)
                    .doOnSuccess(savedAudit -> log.debug("Successfully started audit step with existing messageId: {} for step: {}",
                            messageId, stepName))
                    .doOnError(error -> log.error("Failed to start audit step with existing messageId: {} for step: {}",
                            messageId, stepName, error))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error starting audit step with existing messageId: {} for step: {}", messageId, stepName, e);
        }
    }

    /**
     * Complete audit step successfully
     */
    @Async
    public CompletableFuture<Void> completeStep(UUID messageId, UUID correlationId,
                                                String serviceName, String stepName,
                                                String responsePayload, long processingTimeMs) {

        log.debug("Completing audit step: {} for messageId: {}, processingTime: {}ms",
                stepName, messageId, processingTimeMs);

        try {
            // Find existing audit record to update
            messageAuditRepository.findLatestStepByMessageIdAndStepName(messageId, stepName)
                    .switchIfEmpty(createNewAuditRecord(messageId, correlationId, serviceName, stepName))
                    .flatMap(existingAudit -> {
                        existingAudit.setStatus("COMPLETED");
                        existingAudit.setResponsePayload(truncatePayload(responsePayload));
                        existingAudit.setProcessingTimeMs(processingTimeMs);
                        existingAudit.setCompletedAt(LocalDateTime.now());
                        return messageAuditRepository.save(existingAudit);
                    })
                    .doOnSuccess(savedAudit -> log.debug("Successfully completed audit step: {} with messageId: {}",
                            stepName, messageId))
                    .doOnError(error -> log.error("Failed to complete audit step: {} with messageId: {}",
                            stepName, messageId, error))
                    .subscribe();

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error completing audit step: {} for messageId: {}", stepName, messageId, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Fail audit step with error information
     */
    @Async
    public CompletableFuture<Void> failStep(UUID messageId, UUID correlationId,
                                            String serviceName, String stepName,
                                            String errorMessage, long processingTimeMs) {

        log.debug("Failing audit step: {} for messageId: {}, error: {}", stepName, messageId, errorMessage);

        try {
            // Find existing audit record to update
            messageAuditRepository.findLatestStepByMessageIdAndStepName(messageId, stepName)
                    .switchIfEmpty(createNewAuditRecord(messageId, correlationId, serviceName, stepName))
                    .flatMap(existingAudit -> {
                        existingAudit.setStatus("FAILED");
                        existingAudit.setErrorMessage(truncatePayload(errorMessage));
                        existingAudit.setProcessingTimeMs(processingTimeMs);
                        existingAudit.setFailedAt(LocalDateTime.now());
                        return messageAuditRepository.save(existingAudit);
                    })
                    .doOnSuccess(savedAudit -> log.debug("Successfully failed audit step: {} with messageId: {}",
                            stepName, messageId))
                    .doOnError(error -> log.error("Failed to record audit failure: {} with messageId: {}",
                            stepName, messageId, error))
                    .subscribe();

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error failing audit step: {} for messageId: {}", stepName, messageId, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Create new audit record when one doesn't exist
     */
    private Mono<MessageAudit> createNewAuditRecord(UUID messageId, UUID correlationId,
                                                    String serviceName, String stepName) {
        return Mono.fromSupplier(() -> {
            log.warn("Creating new audit record for messageId: {} as existing record not found", messageId);
            return MessageAudit.builder()
                    .messageId(messageId)
                    .correlationId(correlationId)
                    .serviceName(serviceName)
                    .stepName(stepName)
                    .status("IN_PROGRESS")
                    .createdAt(LocalDateTime.now())
                    .stepSequence(calculateStepSequence(correlationId))
                    .build();
        });
    }

    /**
     * Calculate step sequence based on correlation ID
     * This could be enhanced to query the database for proper sequencing
     */
    private Integer calculateStepSequence(UUID correlationId) {
        // For now, return 1. This could be enhanced to:
        // 1. Query database for max sequence by correlationId
        // 2. Return nextSequence + 1
        return 1;
    }

    /**
     * Truncate payload to prevent database issues
     */
    private String truncatePayload(String payload) {
        if (payload == null) return null;
        return payload.length() > 1000 ? payload.substring(0, 1000) + "... (truncated)" : payload;
    }
}