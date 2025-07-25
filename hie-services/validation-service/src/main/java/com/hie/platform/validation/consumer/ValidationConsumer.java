package com.hie.platform.validation.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.minio.service.MinioService;
import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.consumer.helper.AbstractMessageConsumer;
import com.hie.platform.shared.rabbitmq.consumer.processor.MessageProcessor;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.producer.service.RabbitMQService;
import com.hie.platform.validation.processor.ValidationMessageProcessor;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumer for HL7 validation messages
 * Listens to the hl7-processing queue and delegates processing to ValidationMessageProcessor
 * <p>
 * This class extends AbstractMessageConsumer to inherit:
 * - Retry logic with exponential backoff
 * - Dead letter queue handling
 * - MinIO content retrieval
 * - Error handling and logging
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "rabbitmq.consumer.enabled", havingValue = "true")
public class ValidationConsumer extends AbstractMessageConsumer {

    private final ValidationMessageProcessor validationMessageProcessor;

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
     * The @RabbitListener will automatically:
     * 1. Deserialize the message payload
     * 2. Extract delivery tag and channel information
     * 3. Call this method with the parsed parameters
     */
    @RabbitListener(queues = "${rabbitmq.queues.hl7-processing}")
/*    public void handleHL7ProcessingMessage(@Payload String messagePayload,
                                           Message message,
                                           Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {*/
    public void handleHL7ProcessingMessage(QueueMessage queueMessage, Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Starting HL7 validation processing - MessageId: {}", queueMessage.getMessageId());


        Map<String, Object> payload = queueMessage.getPayload();
        String organizationId = (String) payload.get("organizationId");
        String hl7MessageType = (String) payload.get("hl7MessageType");
        String patientId = (String) payload.get("patientId");
        String s3Location = (String) payload.get("s3Location");

        log.debug("Processing HL7 validation - OrganizationId: {}, MessageType: {}, PatientId: {}, s3Location: {}",
                organizationId, hl7MessageType, patientId,s3Location);
        String minioPath = (String) payload.get("minioPath");

        log.debug("Processing HL7 validation - OrganizationId: {}, MessageType: {}, PatientId: {}, MinioPath: {}",
                organizationId, hl7MessageType, patientId, minioPath);


        // Delegate to the inherited processMessage method from AbstractMessageConsumer
        // This method will:
        // 1. Parse the QueueMessage from messagePayload
        // 2. Retrieve content from MinIO using the minioPath in payload
        // 3. Call getMessageProcessor().processMessage() with content
        // 4. Handle success (forward to next queue) or failure (retry/DLQ)
        // 5. Manage acknowledgments and error handling
        processMessage(queueMessage, channel, deliveryTag, "HL7_VALIDATION");
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