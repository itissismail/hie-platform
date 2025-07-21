package com.hie.platform.messagerouter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.model.MessageState;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.audit.repository.MessageStateRepository;
import com.hie.platform.shared.minio.model.FileUploadResult;
import com.hie.platform.shared.minio.service.MinioService;
import com.hie.platform.shared.rabbitmq.RabbitMQService;
import com.hie.platform.shared.rabbitmq.model.QueueMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class IngestionService {

    private final MessageStateRepository messageStateRepository;
    private final MinioService minioService;
    private final RabbitMQService rabbitMQService;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:message-router-service}")
    private String serviceName;

    @Autowired
    public IngestionService(MessageStateRepository messageStateRepository,
                            MinioService minioService,
                            RabbitMQService rabbitMQService,
                            ObjectMapper objectMapper) {
        this.messageStateRepository = messageStateRepository;
        this.minioService = minioService;
        this.rabbitMQService = rabbitMQService;
        this.objectMapper = objectMapper;
    }

    /**
     * Complete reactive ingestion pipeline:
     * 1. Upload HL7 message to MinIO
     * 2. Create entry in MessageState table
     * 3. Publish message to RabbitMQ processing queue
     */
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.PROCESSING)
    public Mono<String> ingestReactive(String hl7Message) {
        log.debug("IngestionService.ingestReactive() called");
        log.debug("Processing HL7 message: {}",
                hl7Message != null ? hl7Message.substring(0, Math.min(100, hl7Message.length())) + "..." : "null");

        String correlationId = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();

        // Extract basic info from HL7 message
        String messageType = extractMessageType(hl7Message);
        String patientId = extractPatientId(hl7Message);
        String sourceOrganization = extractSourceOrganization(hl7Message);

     /*   // Chain all operations reactively
        return uploadToMinio(hl7Message, sourceOrganization, correlationId)
                .flatMap(uploadResult -> createMessageStateEntry(messageId, correlationId, messageType,
                        patientId, sourceOrganization, uploadResult))
                .flatMap(messageState -> publishToProcessingQueue(messageState, uploadResult, hl7Message)
                        .map(success -> messageState)) // Return messageState for next step
                .flatMap(messageState -> updateMessageStateAfterPublish(messageId, correlationId))
                .map(messageState -> {
                    log.debug("Complete processing pipeline finished successfully. CorrelationId: {}, MessageId: {}",
                            correlationId, messageId);
                    return correlationId;
                })
                .onErrorResume(error -> {
                    log.error("Error in processing pipeline for correlationId: {}", correlationId, error);
                    return createFailedMessageStateEntry(messageId, correlationId, messageType,
                            patientId, sourceOrganization, error.getMessage())
                            .then(Mono.error(error)); // Re-throw error after creating failed entry
                });*/

        return uploadToMinio(hl7Message, sourceOrganization, correlationId)
                .flatMap(uploadResult ->
                        createMessageStateEntry(messageId, correlationId, messageType,
                                patientId, sourceOrganization, uploadResult)
                                .flatMap(messageState ->
                                        // Combine messageState and uploadResult for use in next step
                                        publishToProcessingQueue(messageState, uploadResult, hl7Message)
                                                .map(success -> messageState) // Continue with messageState
                                )
                                .flatMap(messageState ->
                                        updateMessageStateAfterPublish(messageId, correlationId)
                                                .thenReturn(messageState)
                                )
                                .map(messageState -> {
                                    log.debug("Complete processing pipeline finished successfully. CorrelationId: {}, MessageId: {}",
                                            correlationId, messageId);
                                    return correlationId;
                                })
                )
                .onErrorResume(error -> {
                    log.error("Error in processing pipeline for correlationId: {}", correlationId, error);
                    return createFailedMessageStateEntry(messageId, correlationId, messageType,
                            patientId, sourceOrganization, error.getMessage())
                            .then(Mono.error(error));
                });

    }

    /**
     * Process with routing to specific queues based on message type
     */
    public Mono<String> processWithRouting(String hl7Message, String processingType) {
        String correlationId = UUID.randomUUID().toString();
        UUID messageId = UUID.randomUUID();

        String messageType = extractMessageType(hl7Message);
        String patientId = extractPatientId(hl7Message);
        String sourceOrganization = extractSourceOrganization(hl7Message);

      /*  return uploadToMinio(hl7Message, sourceOrganization, correlationId)
                .flatMap(uploadResult -> createMessageStateEntry(messageId, correlationId, messageType,
                        patientId, sourceOrganization, uploadResult))
                .flatMap(messageState -> routeToSpecificQueue(messageState, uploadResult,
                        hl7Message, processingType)
                        .map(success -> messageState))
                .flatMap(messageState -> updateMessageStateAfterPublish(messageId, correlationId))
                .map(success -> correlationId)
                .onErrorResume(error -> {
                    log.error("Error in routing pipeline for correlationId: {}", correlationId, error);
                    return createFailedMessageStateEntry(messageId, correlationId, messageType,
                            patientId, sourceOrganization, error.getMessage())
                            .then(Mono.error(error));
                });*/
        return uploadToMinio(hl7Message, sourceOrganization, correlationId)
                .flatMap(uploadResult ->
                        createMessageStateEntry(messageId, correlationId, messageType,
                                patientId, sourceOrganization, uploadResult)
                                .map(messageState -> Tuples.of(messageState, uploadResult)) // ➕ Carry both forward
                )
                .flatMap(tuple -> {
                    MessageState messageState = tuple.getT1();
                    FileUploadResult uploadResult = tuple.getT2();

                    return routeToSpecificQueue(messageState, uploadResult, hl7Message, processingType)
                            .map(success -> messageState); // Forward only messageState
                })
                .flatMap(messageState -> updateMessageStateAfterPublish(messageId, correlationId))
                .map(success -> correlationId)
                .onErrorResume(error -> {
                    log.error("Error in routing pipeline for correlationId: {}", correlationId, error);
                    return createFailedMessageStateEntry(messageId, correlationId, messageType,
                            patientId, sourceOrganization, error.getMessage())
                            .then(Mono.error(error));
                });

    }

    /**
     * Step 1: Upload HL7 message to MinIO
     */
    private Mono<FileUploadResult> uploadToMinio(String hl7Message, String sourceOrganization, String correlationId) {
        log.debug("Starting MinIO upload for correlationId: {}", correlationId);

        return minioService.uploadFile(hl7Message, sourceOrganization, correlationId, "hl7", "text/plain")
                .doOnSuccess(result -> log.debug("MinIO upload completed successfully. Path: {}, CorrelationId: {}",
                        result.getMinioPath(), correlationId))
                .doOnError(error -> log.error("MinIO upload failed for correlationId: {}", correlationId, error));
    }

    /**
     * Step 2: Create entry in MessageState table (REACTIVE)
     */
    private Mono<MessageState> createMessageStateEntry(UUID messageId, String correlationId,
                                                       String messageType, String patientId,
                                                       String sourceOrganization, FileUploadResult uploadResult) {

        log.debug("Creating message state entry for messageId: {}", messageId);

        MessageState messageState = MessageState.builder()
                .messageId(messageId)
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

        return messageStateRepository.save(messageState)
                .doOnSuccess(savedState -> log.debug("Message state entry created with id: {} for correlationId: {}",
                        savedState.getId(), correlationId))
                .doOnError(error -> log.error("Failed to create message state entry for messageId: {}",
                        messageId, error));
    }

    /**
     * Step 3: Publish message to RabbitMQ processing queue
     */
    private Mono<Boolean> publishToProcessingQueue(MessageState messageState, FileUploadResult uploadResult,
                                                   String hl7Message) {

        log.debug("Publishing to processing queue for messageId: {}", messageState.getMessageId());

        // Create additional processing data
        Map<String, Object> additionalData = createAdditionalProcessingData(hl7Message, uploadResult);

        // Create queue message using the RabbitMQ service helper method
        QueueMessage queueMessage = rabbitMQService.createHL7ProcessingMessage(
                messageState.getMessageId().toString(), // Using messageId as correlationId
                messageState.getSourceOrganization(),
                messageState.getMessageType(),
                messageState.getPatientId(),
                uploadResult.getMinioPath(),
                uploadResult.getS3Location(),
                additionalData
        );

        // Set additional metadata
        queueMessage.setCreatedAt(LocalDateTime.now());
        queueMessage.setHeaders(createProcessingHeaders(messageState, uploadResult));

        return rabbitMQService.sendToProcessingQueue(queueMessage)
                .doOnSuccess(success -> log.debug("Message published successfully to processing queue. MessageId: {}",
                        messageState.getMessageId()))
                .doOnError(error -> log.error("Failed to publish message to processing queue. MessageId: {}",
                        messageState.getMessageId(), error));
    }

    /**
     * Route to specific queue based on processing type
     */
    private Mono<Boolean> routeToSpecificQueue(MessageState messageState, FileUploadResult uploadResult,
                                               String hl7Message, String processingType) {

        Map<String, Object> additionalData = createAdditionalProcessingData(hl7Message, uploadResult);

        QueueMessage queueMessage = rabbitMQService.createHL7ProcessingMessage(
                messageState.getMessageId().toString(),
                messageState.getSourceOrganization(),
                messageState.getMessageType(),
                messageState.getPatientId(),
                uploadResult.getMinioPath(),
                uploadResult.getS3Location(),
                additionalData
        );

        queueMessage.setCreatedAt(LocalDateTime.now());
        queueMessage.setHeaders(createProcessingHeaders(messageState, uploadResult));

        // Route based on processing type
        return switch (processingType.toUpperCase()) {
            case "VALIDATION" -> rabbitMQService.sendToValidationQueue(queueMessage);
            case "CONVERSION" -> rabbitMQService.sendToConversionQueue(queueMessage);
            case "STORAGE" -> rabbitMQService.sendToStorageQueue(queueMessage);
            case "PROCESSING" -> rabbitMQService.sendToProcessingQueue(queueMessage);
            default -> {
                log.warn("Unknown processing type: {}. Routing to default processing queue.", processingType);
                yield rabbitMQService.sendToProcessingQueue(queueMessage);
            }
        };
    }

    /**
     * Update message state after successful publishing (REACTIVE)
     */
    private Mono<MessageState> updateMessageStateAfterPublish(UUID messageId, String correlationId) {
        log.debug("Updating message state after successful publish for messageId: {}", messageId);

        return messageStateRepository.findByMessageId(messageId)
                .switchIfEmpty(Mono.error(new RuntimeException("MessageState not found for messageId: " + messageId)))
                .flatMap(messageState -> {
                    messageState.setCurrentStatus(MessageStatus.QUEUED.name());
                    messageState.setUpdatedAt(LocalDateTime.now());
                    messageState.setLastProcessedBy(serviceName);

                    return messageStateRepository.save(messageState);
                })
                .doOnSuccess(updatedState -> log.debug("Message state updated successfully for messageId: {}", messageId))
                .doOnError(error -> log.error("Failed to update message state for messageId: {}", messageId, error));
    }

    /**
     * Create failed message state entry (REACTIVE)
     */
    private Mono<MessageState> createFailedMessageStateEntry(UUID messageId, String correlationId,
                                                             String messageType, String patientId,
                                                             String sourceOrganization, String errorMessage) {

        log.debug("Creating failed message state entry for messageId: {}", messageId);

        MessageState messageState = MessageState.builder()
                .messageId(messageId)
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

        return messageStateRepository.save(messageState)
                .doOnError(error -> log.error("Failed to create failed message state entry for messageId: {}",
                        messageId, error));
    }

    /**
     * Create additional processing data for queue message
     */
    private Map<String, Object> createAdditionalProcessingData(String hl7Message, FileUploadResult uploadResult) {
        Map<String, Object> additionalData = new HashMap<>();

        // File metadata
        additionalData.put("fileSize", uploadResult.getFileSize());
        additionalData.put("contentType", uploadResult.getContentType());
        additionalData.put("uploadedAt", uploadResult.getUploadedAt().toString());
        additionalData.put("bucketName", uploadResult.getBucketName());

        // Message metadata
        if (hl7Message != null) {
            additionalData.put("hl7MessageLength", hl7Message.length());
            additionalData.put("hl7Preview", hl7Message.length() > 500 ?
                    hl7Message.substring(0, 500) + "..." : hl7Message);
        }

        // Processing metadata
        additionalData.put("processedBy", serviceName);
        additionalData.put("processedAt", LocalDateTime.now().toString());
        additionalData.put("ingestionTimestamp", LocalDateTime.now().toString());

        return additionalData;
    }

    /**
     * Create processing headers for queue message
     */
    private Map<String, String> createProcessingHeaders(MessageState messageState, FileUploadResult uploadResult) {
        Map<String, String> headers = new HashMap<>();

        headers.put("messageId", messageState.getMessageId().toString());
        headers.put("messageType", messageState.getMessageType());
        headers.put("sourceOrganization", messageState.getSourceOrganization());
        headers.put("patientId", messageState.getPatientId() != null ? messageState.getPatientId() : "");
        headers.put("s3Location", uploadResult.getS3Location());
        headers.put("minioPath", uploadResult.getMinioPath());
        headers.put("processedBy", serviceName);
        headers.put("ingestionTimestamp", LocalDateTime.now().toString());

        return headers;
    }

    // HL7 parsing utility methods
    private String extractMessageType(String hl7Message) {
        if (hl7Message == null || hl7Message.isEmpty()) {
            return "UNKNOWN";
        }

        String[] segments = hl7Message.split("\\r|\\n");
        for (String segment : segments) {
            if (segment.startsWith("MSH")) {
                String[] fields = segment.split("\\|");
                if (fields.length > 8) {
                    return fields[8].split("\\^")[0];
                }
            }
        }
        return "UNKNOWN";
    }

    private String extractPatientId(String hl7Message) {
        if (hl7Message == null || hl7Message.isEmpty()) {
            return null;
        }

        String[] segments = hl7Message.split("\\r|\\n");
        for (String segment : segments) {
            if (segment.startsWith("PID")) {
                String[] fields = segment.split("\\|");
                if (fields.length > 3) {
                    return fields[3].split("\\^")[0];
                }
            }
        }
        return null;
    }

    private String extractSourceOrganization(String hl7Message) {
        if (hl7Message == null || hl7Message.isEmpty()) {
            return "UNKNOWN";
        }

        String[] segments = hl7Message.split("\\r|\\n");
        for (String segment : segments) {
            if (segment.startsWith("MSH")) {
                String[] fields = segment.split("\\|");
                if (fields.length > 3) {
                    return fields[3];
                }
            }
        }
        return "UNKNOWN";
    }

    // Legacy method for backward compatibility
    @Deprecated
    public String ingest(String hl7Message) {
        log.debug("Legacy ingest() method called - delegating to reactive version");
        return ingestReactive(hl7Message).block();
    }
}