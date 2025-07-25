package com.hie.platform.validation.processor;

import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.consumer.processor.MessageProcessor;
import com.hie.platform.shared.rabbitmq.consumer.processor.ProcessingResult;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.validation.service.HL7ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Message processor implementation for HL7 validation
 *
 * This class contains the business logic for processing HL7 validation messages.
 * It implements the MessageProcessor interface and is used by ValidationConsumer
 * through the AbstractMessageConsumer framework.
 *
 * Responsibilities:
 * - Validate input parameters and message metadata
 * - Delegate HL7 validation to HL7ValidationService
 * - Prepare processing results for next stage
 * - Handle validation success/failure scenarios
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ValidationMessageProcessor implements MessageProcessor {

    private final HL7ValidationService hl7ValidationService;
    private final RabbitMQProperties rabbitMQProperties;

    /**
     * Process HL7 validation message
     *
     * This method is called by AbstractMessageConsumer after:
     * - Message parsing from JSON
     * - Content retrieval from MinIO
     * - Retry logic setup
     *
     * @param queueMessage The parsed queue message with metadata
     * @param messageContent The actual HL7 content retrieved from MinIO
     * @return ProcessingResult indicating success/failure and next steps
     */
    @Override
    public ProcessingResult processMessage(QueueMessage queueMessage, String messageContent) {
        try {
            log.info("Starting HL7 validation processing - MessageId: {}", queueMessage.getMessageId());

            // Extract and validate metadata from payload
            Map<String, Object> payload = queueMessage.getPayload();
            String organizationId = (String) payload.get("organizationId");
            String hl7MessageType = (String) payload.get("hl7MessageType");
            String patientId = (String) payload.get("patientId");
            String minioPath = (String) payload.get("minioPath");

            log.debug("Processing HL7 validation - OrganizationId: {}, MessageType: {}, PatientId: {}, MinioPath: {}",
                    organizationId, hl7MessageType, patientId, minioPath);

            // Validate required metadata
            ProcessingResult validationResult = validateRequiredFields(organizationId, hl7MessageType, minioPath);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }

            // Validate message content availability
            if (messageContent == null || messageContent.trim().isEmpty()) {
                return ProcessingResult.failure("Message content is empty or null - failed to retrieve from MinIO path: " + minioPath);
            }

            log.debug("Retrieved message content from MinIO - Size: {} bytes", messageContent.length());

            // Perform HL7 validation using the validation service
            boolean validationSuccess = hl7ValidationService.validateHL7Message(queueMessage, messageContent);

            if (validationSuccess) {
                return handleValidationSuccess(queueMessage, payload);
            } else {
                return handleValidationFailure(queueMessage);
            }

        } catch (Exception e) {
            log.error("Exception during HL7 validation processing - MessageId: {}",
                    queueMessage.getMessageId(), e);
            return ProcessingResult.failure("Validation processing failed due to exception", e);
        }
    }

    /**
     * Validate required fields in message payload
     */
    private ProcessingResult validateRequiredFields(String organizationId, String hl7MessageType, String minioPath) {
        if (organizationId == null || organizationId.isEmpty()) {
            return ProcessingResult.failure("Missing organizationId in message payload");
        }

        if (hl7MessageType == null || hl7MessageType.isEmpty()) {
            return ProcessingResult.failure("Missing hl7MessageType in message payload");
        }

        if (minioPath == null || minioPath.isEmpty()) {
            return ProcessingResult.failure("Missing minioPath in message payload");
        }

        return ProcessingResult.success();
    }

    /**
     * Handle successful validation
     */
    private ProcessingResult handleValidationSuccess(QueueMessage queueMessage, Map<String, Object> payload) {
        log.info("HL7 validation successful - MessageId: {}", queueMessage.getMessageId());

        // Get the next queue for successful processing (hl7-validation queue)
        String nextQueue = rabbitMQProperties.getQueues().getHl7Validation();

        // Add validation metadata to the payload for downstream services
        payload.put("validationStatus", "VALIDATED");
        payload.put("validatedAt", LocalDateTime.now().toString());
        payload.put("validatedBy", "validation-service");
        payload.put("validationServiceVersion", "1.0.0");

        log.debug("Forwarding validated message to next queue: {} - MessageId: {}",
                nextQueue, queueMessage.getMessageId());

        return ProcessingResult.success(nextQueue, payload);
    }

    /**
     * Handle validation failure
     */
    private ProcessingResult handleValidationFailure(QueueMessage queueMessage) {
        log.error("HL7 validation failed - MessageId: {}", queueMessage.getMessageId());

        // Add failure metadata to payload for analysis
        Map<String, Object> payload = queueMessage.getPayload();
        payload.put("validationStatus", "FAILED");
        payload.put("validationFailedAt", LocalDateTime.now().toString());
        payload.put("validatedBy", "validation-service");

        return ProcessingResult.failure("HL7 validation failed - check logs for detailed validation errors");
    }

    /**
     * Get processor name for logging purposes
     */
    @Override
    public String getProcessorName() {
        return "HL7ValidationProcessor";
    }
}