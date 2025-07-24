package com.hie.platform.messagerouter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.audit.annotation.AuditStep;
import com.hie.platform.shared.audit.model.MessageState;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.message.service.MessageStateService;
import com.hie.platform.shared.minio.model.FileUploadResult;
import com.hie.platform.shared.minio.service.MinioService;
import com.hie.platform.shared.rabbitmq.producer.service.RabbitMQService;

import com.hie.platform.shared.util.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class IngestionService {

    private final MessageStateService messageStateService;
    private final MinioService minioService;
    private final RabbitMQService rabbitMQService;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:message-router-service}")
    private String serviceName;

    @Autowired
    public IngestionService(MessageStateService messageStateService,
                            MinioService minioService,
                            RabbitMQService rabbitMQService,
                            ObjectMapper objectMapper) {
        this.messageStateService = messageStateService;
        this.minioService = minioService;
        this.rabbitMQService = rabbitMQService;
        this.objectMapper = objectMapper;
    }

    /**
     * Complete reactive ingestion pipeline:
     * 1. Upload HL7 message to MinIO
     * 2. Create entry in MessageState table.
     * 3. Publish message to RabbitMQ processing queue
     */
    @AuditStep(serviceName = "message-router-service", stepName = MessageStatus.PROCESSING)
    public Mono<String> ingestReactive(String hl7Message, String messageIdStr, String correlationId) {
        log.debug(" Processing HL7 message in ingest-reactive: {}",
                hl7Message != null ? hl7Message.substring(0, Math.min(100, hl7Message.length())) + "..." : "null");

        UUID messageId = UUID.fromString(messageIdStr);
        // Extract basic info from HL7 message
        String messageType = extractMessageType(hl7Message);
        String patientId = extractPatientId(hl7Message);
        String sourceOrganization = extractSourceOrganization(hl7Message);

        return uploadToMinio(hl7Message, sourceOrganization, messageId.toString())
                .flatMap(uploadResult ->
                        messageStateService.createMessageStateEntry(messageId, UUID.fromString(correlationId), messageType,
                                        patientId, sourceOrganization, uploadResult, serviceName)
                                .flatMap(messageState ->
                                        // Combine messageState and uploadResult for use in next step
                                        rabbitMQService.publishToProcessingQueue(messageState, uploadResult, hl7Message,serviceName,correlationId)
                                                .map(success -> messageState) // Continue with messageState
                                )
                                .flatMap(messageState ->
                                        messageStateService.updateMessageStateAfterPublish(messageId, correlationId, serviceName)
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
                    return messageStateService.createFailedMessageStateEntry(messageId, UUID.fromString(correlationId), messageType,
                                    patientId, sourceOrganization, error.getMessage(), serviceName)
                            .then(Mono.error(error));
                });

    }

    /**
     * Process with routing to specific queues based on message type
     */
    public Mono<String> processWithRouting(String hl7Message, String processingType,String messageIdStr, String correlationId) {
        UUID messageId = UUID.fromString(messageIdStr);

        String messageType = extractMessageType(hl7Message);
        String patientId = extractPatientId(hl7Message);
        String sourceOrganization = extractSourceOrganization(hl7Message);

        return uploadToMinio(hl7Message, sourceOrganization, correlationId)
                .flatMap(uploadResult ->
                        messageStateService.createMessageStateEntry(messageId, UUID.fromString(correlationId), messageType,
                                        patientId, sourceOrganization, uploadResult, serviceName)
                                .map(messageState -> Tuples.of(messageState, uploadResult)) // ➕ Carry both forward
                )
                .flatMap(tuple -> {
                    MessageState messageState = tuple.getT1();
                    FileUploadResult uploadResult = tuple.getT2();

                    return rabbitMQService.routeToSpecificQueue(messageState, uploadResult, hl7Message, processingType,serviceName,correlationId)
                            .map(success -> messageState); // Forward only messageState
                })
                .flatMap(messageState -> messageStateService.updateMessageStateAfterPublish(messageId, correlationId, serviceName))
                .map(success -> correlationId)
                .onErrorResume(error -> {
                    log.error("Error in routing pipeline for correlationId: {}", correlationId, error);
                    return messageStateService.createFailedMessageStateEntry(messageId, UUID.fromString(correlationId), messageType,
                                    patientId, sourceOrganization, error.getMessage(), serviceName)
                            .then(Mono.error(error));
                });

    }

    /**
     * Step 1: Upload HL7 message to MinIO
     */
    private Mono<FileUploadResult> uploadToMinio(String hl7Message, String sourceOrganization, String messageId) {
        log.debug("Starting MinIO upload for messageId: {}", messageId);
        String minioPath = generateStructuredPath(sourceOrganization,"raw", messageId, AppConstant.FILE_EXTENSION_TXT);
        log.info("Generated MinIO path: {}", minioPath);

        return minioService.uploadFile(hl7Message, sourceOrganization, messageId, "txt", "text/plain",minioPath)
                .doOnSuccess(result -> log.debug("MinIO upload completed successfully. Path: {}, CorrelationId: {}",
                        result.getMinioPath(), messageId))
                .doOnError(error -> log.error("MinIO upload failed for correlationId: {}", messageId, error));
    }

    /**
     * Step 2: Create entry in MessageState table (REACTIVE)
     */


    /**
     * Step 3: Publish message to RabbitMQ processing queue
     */

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

    private String generateStructuredPath(String organizationId,String queueType, String messageId, String fileExtension) {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MMM"));

        String day = now.format(DateTimeFormatter.ofPattern("dd"));

        String sanitizedOrgId = sanitizeOrganizationId(organizationId);
        String extension = fileExtension.startsWith(".") ? fileExtension : "." + fileExtension;

        return String.format("%s/%s/%s/%s/%s/%s%s", sanitizedOrgId, queueType, year, month, day, messageId, extension);
    }

    /**
     * Generate structured MinIO path: organization-id/year/month/day/correlationId.ext
     */


    /**
     * Sanitize organization ID for filesystem safety
     */
    private String sanitizeOrganizationId(String organizationId) {
        if (organizationId == null || organizationId.trim().isEmpty()) {
            return "unknown-org";
        }

        return organizationId.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\-_]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

}