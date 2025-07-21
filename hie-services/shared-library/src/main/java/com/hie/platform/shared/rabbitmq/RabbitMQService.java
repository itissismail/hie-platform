// ============================================================================
// 2. RabbitMQ Service Wrapper
// ============================================================================

package com.hie.platform.shared.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.rabbitmq.config.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.exception.RabbitMQServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitMQProperties rabbitMQProperties;

    @Autowired
    public RabbitMQService(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                           RabbitMQProperties rabbitMQProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.rabbitMQProperties = rabbitMQProperties;
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
     * Generic send message method
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

                // Create message properties
                MessageProperties properties = new MessageProperties();
                properties.setContentType("application/json");
                //properties.setDeliveryMode(MessageProperties.DELIVERY_MODE_PERSISTENT);
                properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);
                properties.setMessageId(queueMessage.getMessageId());
                properties.setCorrelationId(queueMessage.getCorrelationId());
                properties.setTimestamp(java.util.Date.from(
                        queueMessage.getSentAt().atZone(java.time.ZoneId.systemDefault()).toInstant()));

                // Create message
                Message message = new Message(jsonPayload.getBytes(), properties);

                // Send message
                rabbitTemplate.send(queueName, message);

                log.debug("Message sent successfully to queue: {} with correlationId: {}",
                        queueName, queueMessage.getCorrelationId());

                return true;
            } catch (Exception e) {
                log.error("Failed to send message to queue: {} with correlationId: {}",
                        queueName, queueMessage.getCorrelationId(), e);
                throw new RabbitMQServiceException("Failed to send message to queue: " + queueName, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Send message to exchange with routing key
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
                properties.setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE);
                properties.setMessageId(queueMessage.getMessageId());
                properties.setCorrelationId(queueMessage.getCorrelationId());

                Message message = new Message(jsonPayload.getBytes(), properties);

                rabbitTemplate.send(exchangeName, routingKey, message);

                log.debug("Message sent successfully to exchange: {} with routingKey: {} and correlationId: {}",
                        exchangeName, routingKey, queueMessage.getCorrelationId());

                return true;
            } catch (Exception e) {
                log.error("Failed to send message to exchange: {} with routingKey: {} and correlationId: {}",
                        exchangeName, routingKey, queueMessage.getCorrelationId(), e);
                throw new RabbitMQServiceException("Failed to send message to exchange", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Create HL7 message for processing queue
     */
    public QueueMessage createHL7ProcessingMessage(String correlationId, String organizationId,
                                                   String messageType, String patientId,
                                                   String minioPath, String s3Location,
                                                   Map<String, Object> additionalData) {
        return QueueMessage.builder()
                .correlationId(correlationId)
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
                .maxRetries(3)
                .build();
    }
}