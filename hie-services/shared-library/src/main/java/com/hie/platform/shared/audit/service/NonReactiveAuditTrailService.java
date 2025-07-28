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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NonReactiveAuditTrailService {

    private final MessageAuditRepository messageAuditRepository;

    /**
     * Start audit step with new message ID while preserving correlation ID
     *
     * @param correlationId Original correlation ID to maintain
     * @param previousMessageId Previous message ID from upstream service
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
                    .stepSequence(1) // Will be updated by database trigger or service
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
     * Complete audit step successfully
     */
    @Async
    public CompletableFuture<Void> completeStep(UUID messageId, UUID correlationId,
                                                String serviceName, String stepName,
                                                String responsePayload, long processingTimeMs) {

        log.debug("Completing audit step: {} for messageId: {}, processingTime: {}ms",
                stepName, messageId, processingTimeMs);

        try {
            // First try to find existing audit record to update
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
            // First try to find existing audit record to update
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
     * Synchronous version of startStep for immediate use
     */
    public UUID startStepSync(UUID correlationId, UUID previousMessageId,
                              String serviceName, String stepName, String requestPayload) {
        UUID newMessageId = UUID.randomUUID();

        log.debug("Starting audit step synchronously: {} for service: {}, newMessageId: {}",
                stepName, serviceName, newMessageId);

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
                    .stepSequence(1)
                    .build();

            messageAuditRepository.save(audit)
                    .doOnSuccess(savedAudit -> log.debug("Successfully started audit step synchronously: {}", stepName))
                    .doOnError(error -> log.error("Failed to start audit step synchronously: {}", stepName, error))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error starting audit step synchronously: {} for messageId: {}", stepName, newMessageId, e);
        }

        return newMessageId;
    }

    /**
     * Create new audit record when one doesn't exist
     */
    private Mono<MessageAudit> createNewAuditRecord(UUID messageId, UUID correlationId,
                                                    String serviceName, String stepName) {
        return Mono.fromSupplier(() -> MessageAudit.builder()
                .messageId(messageId)
                .correlationId(correlationId)
                .serviceName(serviceName)
                .stepName(stepName)
                .status("IN_PROGRESS")
                .createdAt(LocalDateTime.now())
                .stepSequence(1)
                .build());
    }

    /**
     * Truncate payload to prevent database issues
     */
    private String truncatePayload(String payload) {
        if (payload == null) return null;
        return payload.length() > 1000 ? payload.substring(0, 1000) + "... (truncated)" : payload;
    }
}