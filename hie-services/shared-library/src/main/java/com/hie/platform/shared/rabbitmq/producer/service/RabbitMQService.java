package com.hie.platform.shared.rabbitmq.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.audit.model.MessageState;
import com.hie.platform.shared.minio.model.FileUploadResult;
import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.producer.exception.RabbitMQServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service("rabbitMQService")
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitMQProperties rabbitMQProperties;

    @Autowired
    public RabbitMQService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                           RabbitMQProperties rabbitMQProperties){
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.rabbitMQProperties = rabbitMQProperties;

        // Log configuration on startup
        log.info("RabbitMQ Service initialized with persistence: {}, durable: {}",
                rabbitMQProperties.getMessageSettings().isPersistent(),
                rabbitMQProperties.getMessageSettings().isDurable());
    }

    public Mono<Boolean> publishToProcessingQueue(MessageState messageState, FileUploadResult uploadResult,
                                                  String hl7Message, String serviceName,String correlationId) {

        log.debug("Publishing to processing queue for messageId: {}", messageState.getMessageId());

        // Create additional processing data
        Map<String, Object> additionalData = createAdditionalProcessingData(hl7Message, uploadResult,serviceName);

        // Create queue message using the RabbitMQ service helper method
        QueueMessage queueMessage = this.createHL7ProcessingMessage(
                correlationId,
                messageState.getSourceOrganization(),
                messageState.getMessageType(),
                messageState.getPatientId(),
                uploadResult.getMinioPath(),
                uploadResult.getS3Location(),
                additionalData
        );

        // Set additional metadata
        queueMessage.setCreatedAt(LocalDateTime.now());
        queueMessage.setHeaders(createProcessingHeaders(messageState, uploadResult,serviceName));

        return this.sendToProcessingQueue(queueMessage)
                .doOnSuccess(success -> log.debug("Message published successfully to processing queue. MessageId: {}",
                        messageState.getMessageId()))
                .doOnError(error -> log.error("Failed to publish message to processing queue. MessageId: {}",
                        messageState.getMessageId(), error));
    }

    /**
     * Route to specific queue based on processing type
     */
    public Mono<Boolean> routeToSpecificQueue(MessageState messageState, FileUploadResult uploadResult,
                                              String hl7Message, String processingType, String serviceName, String correlationId) {

        Map<String, Object> additionalData = createAdditionalProcessingData(hl7Message, uploadResult,serviceName);

        QueueMessage queueMessage = this.createHL7ProcessingMessage(
                correlationId,
                messageState.getSourceOrganization(),
                messageState.getMessageType(),
                messageState.getPatientId(),
                uploadResult.getMinioPath(),
                uploadResult.getS3Location(),
                additionalData
        );

        queueMessage.setCreatedAt(LocalDateTime.now());
        queueMessage.setHeaders(createProcessingHeaders(messageState, uploadResult,serviceName));

        // Route based on processing type
        return switch (processingType.toUpperCase()) {
            case "VALIDATION" -> this.sendToValidationQueue(queueMessage);
            case "CONVERSION" -> this.sendToConversionQueue(queueMessage);
            case "STORAGE" -> this.sendToStorageQueue(queueMessage);
            case "PROCESSING" -> this.sendToProcessingQueue(queueMessage);
            default -> {
                log.warn("Unknown processing type: {}. Routing to default processing queue.", processingType);
                yield this.sendToProcessingQueue(queueMessage);
            }
        };
    }





    /**
     * Send message to HL7 Processing Queue
     */
    public Mono<Boolean> sendToProcessingQueue(QueueMessage queueMessage) {
        return sendMessage(queueMessage, rabbitMQProperties.getQueues().getHl7Processing());
    }

    /**
     * Send message to HL7 Validation Queue
     */
    public Mono<Boolean> sendToValidationQueue(QueueMessage queueMessage) {
        return sendMessage(queueMessage, rabbitMQProperties.getQueues().getHl7Validation());
    }

    /**
     * Send message to HL7 Conversion Queue
     */
    public Mono<Boolean> sendToConversionQueue(QueueMessage queueMessage) {
        return sendMessage(queueMessage, rabbitMQProperties.getQueues().getHl7Conversion());
    }

    /**
     * Send message to HL7 Storage Queue
     */
    public Mono<Boolean> sendToStorageQueue(QueueMessage queueMessage) {
        return sendMessage(queueMessage, rabbitMQProperties.getQueues().getHl7Storage());
    }

    /**
     * Send message to custom queue
     */
    public Mono<Boolean> sendToQueue(QueueMessage queueMessage, String queueName) {
        return sendMessage(queueMessage, queueName);
    }

    /**
     * Send message with routing key
     */
    public Mono<Boolean> sendWithRoutingKey(QueueMessage queueMessage, String routingKey) {
        return sendMessage(queueMessage, rabbitMQProperties.getExchangeName(), routingKey);
    }

    /**
     * Generic send message method with PROPER persistence
     */
    private Mono<Boolean> sendMessage(QueueMessage queueMessage, String queueName) {
        return Mono.fromCallable(() -> {
            try {
                // Enrich message with metadata
                queueMessage.setSentAt(LocalDateTime.now());
                queueMessage.setSentBy(rabbitMQProperties.getServiceName());

                if (queueMessage.getMessageId() == null) {
                    queueMessage.setMessageId(UUID.randomUUID().toString());
                }

                // Convert to JSON
                String jsonPayload = objectMapper.writeValueAsString(queueMessage);

                // Create message properties with PERSISTENT delivery mode
                MessageProperties properties = new MessageProperties();
                properties.setContentType("application/json");

                // CRITICAL: Set delivery mode to PERSISTENT for durability
                if (rabbitMQProperties.getMessageSettings().isPersistent()) {
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    log.debug("Message set to PERSISTENT mode");
                } else {
                    properties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
                    log.debug("Message set to NON_PERSISTENT mode");
                }

                properties.setMessageId(queueMessage.getMessageId());
                properties.setCorrelationId(queueMessage.getCorrelationId());
                properties.setTimestamp(java.util.Date.from(
                        queueMessage.getSentAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));

                // Add custom headers for tracking
                properties.setHeader("sent-by", queueMessage.getSentBy());
                properties.setHeader("retry-count", queueMessage.getRetryCount());
                properties.setHeader("max-retries", queueMessage.getMaxRetries());

                // Set message expiration if TTL is configured
                if (rabbitMQProperties.getMessageSettings().getMessageTtl() > 0) {
                    properties.setExpiration(String.valueOf(rabbitMQProperties.getMessageSettings().getMessageTtl()));
                }

                // Create message
                Message message = new Message(jsonPayload.getBytes(), properties);

                // Send message directly to queue (not through exchange)
                rabbitTemplate.send(queueName, message);

                log.info("Message sent successfully to queue: {} with messageId: {}, correlationId: {}, persistent: {}",
                        queueName, queueMessage.getMessageId(), queueMessage.getCorrelationId(),
                        rabbitMQProperties.getMessageSettings().isPersistent());

                return true;
            } catch (Exception e) {
                log.error("Failed to send message to queue: {} with messageId: {}, correlationId: {}",
                        queueName, queueMessage.getMessageId(), queueMessage.getCorrelationId(), e);
                throw new RabbitMQServiceException("Failed to send message to queue: " + queueName, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Send message to exchange with routing key and PROPER persistence
     */
    private Mono<Boolean> sendMessage(QueueMessage queueMessage, String exchangeName, String routingKey) {
        return Mono.fromCallable(() -> {
            try {
                queueMessage.setSentAt(LocalDateTime.now());
                queueMessage.setSentBy(rabbitMQProperties.getServiceName());

                if (queueMessage.getMessageId() == null) {
                    queueMessage.setMessageId(UUID.randomUUID().toString());
                }

                String jsonPayload = objectMapper.writeValueAsString(queueMessage);

                MessageProperties properties = new MessageProperties();
                properties.setContentType("application/json");

                // CRITICAL: Set delivery mode to PERSISTENT
                if (rabbitMQProperties.getMessageSettings().isPersistent()) {
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                } else {
                    properties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
                }

                properties.setMessageId(queueMessage.getMessageId());
                properties.setCorrelationId(queueMessage.getCorrelationId());
                properties.setTimestamp(java.util.Date.from(
                        queueMessage.getSentAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));

                // Add routing and tracking headers
                properties.setHeader("sent-by", queueMessage.getSentBy());
                properties.setHeader("routing-key", routingKey);

                Message message = new Message(jsonPayload.getBytes(), properties);

                rabbitTemplate.send(exchangeName, routingKey, message);

                log.info("Message sent successfully to exchange: {} with routingKey: {}, messageId: {}, correlationId: {}, persistent: {}",
                        exchangeName, routingKey, queueMessage.getMessageId(), queueMessage.getCorrelationId(),
                        rabbitMQProperties.getMessageSettings().isPersistent());

                return true;
            } catch (Exception e) {
                log.error("Failed to send message to exchange: {} with routingKey: {}, messageId: {}, correlationId: {}",
                        exchangeName, routingKey, queueMessage.getMessageId(), queueMessage.getCorrelationId(), e);
                throw new RabbitMQServiceException("Failed to send message to exchange", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Create HL7 message for processing queue with proper IDs
     */
    public QueueMessage createHL7ProcessingMessage(String correlationId, String organizationId,
                                                   String messageType, String patientId,
                                                   String minioPath, String s3Location,
                                                   Map<String, Object> additionalData) {
        return QueueMessage.builder()
                .messageId(UUID.randomUUID().toString())  // Unique per message
                .correlationId(correlationId)             // Shared across related messages
                .messageType("HL7_PROCESSING")
                .payload(Map.of(
                        "organizationId", organizationId,
                        "hl7MessageType", messageType,
                        "patientId", patientId,
                        "minioPath", minioPath,
                        "s3Location", s3Location,
                        "additionalData", additionalData != null ? additionalData : Map.of()
                ))
                .priority(1)
                .retryCount(0)
                .maxRetries(rabbitMQProperties.getMessageSettings().getMaxRetries())
                .build();
    }

    /**
     * Create a new correlation ID for a business transaction
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Check if RabbitMQ is configured for persistence
     */
    public boolean isPersistenceEnabled() {
        return rabbitMQProperties.getMessageSettings().isPersistent() &&
                rabbitMQProperties.getMessageSettings().isDurable();
    }

    /**
     * Create additional processing data for queue message
     */
    private Map<String, Object> createAdditionalProcessingData(String hl7Message, FileUploadResult uploadResult, String serviceName) {
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
    private Map<String, String> createProcessingHeaders(MessageState messageState, FileUploadResult uploadResult, String serviceName) {
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

}