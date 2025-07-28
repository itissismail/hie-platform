package com.hie.platform.validation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.audit.annotation.NonReactiveAuditStep;
import com.hie.platform.shared.audit.model.MessageStatus;
import com.hie.platform.shared.minio.service.MinioService;
import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.consumer.helper.AbstractMessageConsumer;
import com.hie.platform.shared.rabbitmq.consumer.processor.MessageProcessor;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.producer.service.RabbitMQService;
import com.hie.platform.validation.processor.ValidationMessageProcessor;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumer for HL7 validation messages
 * Listens to the hl7-processing queue and delegates processing to ValidationMessageProcessor
 * <p>
 * This class extends AbstractMessageConsumer to inherit:
 * - Retry logic with exponential backoff
 * - Dead letter queue handling
 * - MinIO content retrieval
 * - Error handling and logging
 *
 * UPDATED: Now uses @NonReactiveAuditStep for proper non-reactive audit logging
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "rabbitmq.consumer.enabled", havingValue = "true")
public class ValidationConsumer extends AbstractMessageConsumer {

    private final ValidationMessageProcessor validationMessageProcessor;

    @Value("${rabbitmq.service-name}")
    String serviceName;

    public ValidationConsumer(ObjectMapper objectMapper,
                              RabbitMQProperties rabbitMQProperties,
                              MinioService minioService,
                              RabbitMQService rabbitMQService,
                              ValidationMessageProcessor validationMessageProcessor) {
        super(objectMapper, rabbitMQProperties, minioService, rabbitMQService);
        this.validationMessageProcessor = validationMessageProcessor;
    }

    /**
     * Listen to HL7 processing raw queue
     * This is where Message-Router publishes messages after uploading to MinIO
     * <p>
     * UPDATED: Changed from @AuditStep to @NonReactiveAuditStep for proper non-reactive audit handling
     * The @RabbitListener will automatically:
     * 1. Deserialize the message payload
     * 2. Extract delivery tag and channel information
     * 3. Call this method with the parsed parameters
     *
     * ValidationConsumer (Entry Point)
     *     ↓ extends
     * AbstractMessageConsumer (Framework Logic)
     *     ↓ uses
     * ValidationMessageProcessor (Business Logic)
     *     ↓ uses
     * HL7ValidationService (Domain Logic)
     */
    @RabbitListener(queues = "${rabbitmq.queues.hl7-processing}")
    @NonReactiveAuditStep(serviceName = "Validation-Service", stepName = MessageStatus.VALIDATED)
    public void handleHL7ProcessingMessage(QueueMessage queueMessage, Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Starting HL7 validation processing - MessageId: {} , correlationId: {}",
                queueMessage.getMessageId(), queueMessage.getCorrelationId());

        // Store original messageId as previousMessageId for audit trail
        String originalMessageId = queueMessage.getMessageId();
        String correlationId = queueMessage.getCorrelationId();

        // Generate new messageId for this service while preserving correlationId
        String newMessageId = UUID.randomUUID().toString();

        log.debug("Generated new messageId: {} for validation service, originalMessageId: {}, correlationId: {}",
                newMessageId, originalMessageId, correlationId);

        // Update QueueMessage with new messageId (preserving correlationId)
        queueMessage.setMessageId(newMessageId);
        // Keep correlationId as is - queueMessage.setCorrelationId(correlationId); // Already set

        // Add previousMessageId to payload for audit trail
        Map<String, Object> payload = queueMessage.getPayload();
        payload.put("previousMessageId", originalMessageId);

        String organizationId = (String) payload.get("organizationId");
        String hl7MessageType = (String) payload.get("hl7MessageType");
        String patientId = (String) payload.get("patientId");
        String s3Location = (String) payload.get("s3Location");
        String minioPath = (String) payload.get("minioPath");

        log.debug("Processing HL7 validation - OrganizationId: {}, MessageType: {}, PatientId: {}, s3Location: {}, MinioPath: {}",
                organizationId, hl7MessageType, patientId, s3Location, minioPath);

        // Delegate to the inherited processMessage method from AbstractMessageConsumer
        // The @NonReactiveAuditStep annotation will handle:
        // 1. Extract correlationId and previousMessageId from QueueMessage
        // 2. Generate new messageId for this service (already done above)
        // 3. Create audit entry with proper messageId lineage using NonReactiveAuditTrailService
        // 4. Update QueueMessage with new messageId for downstream processing
        // 5. Use ThreadLocal context instead of Reactor context
        processMessage(queueMessage, channel, deliveryTag, serviceName);
    }

    /**
     * Provide the message processor implementation
     * This method is called by AbstractMessageConsumer to get the processor
     * that contains the actual business logic
     *
     * @return MessageProcessor instance that will handle the validation logic
     */
    @Override
    protected MessageProcessor getMessageProcessor() {
        return validationMessageProcessor;
    }
}