package com.hie.platform.shared.rabbitmq.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.producer.exception.MessageProcessingException;
import com.hie.platform.shared.rabbitmq.producer.service.RabbitMQService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;


@Slf4j
//@Service
public class RabbitMQConsumerService {

    private final ObjectMapper objectMapper;
    private final RabbitMQProperties rabbitMQProperties;
    private final RabbitMQService rabbitMQService;

    public RabbitMQConsumerService(ObjectMapper objectMapper,
                                   @Qualifier("rabbitMQProperties") RabbitMQProperties rabbitMQProperties,
                                   RabbitMQService rabbitMQService) {
        this.objectMapper = objectMapper;
        this.rabbitMQProperties = rabbitMQProperties;
        this.rabbitMQService = rabbitMQService;
    }

    /**
     * Process HL7 Processing Queue messages
     */
    @RabbitListener(queues = "#{rabbitMQProperties.queues.hl7Processing}")
    public void processHL7Message(@Payload String messagePayload,
                                  Message message,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        processMessage(messagePayload, message, channel, deliveryTag, "HL7_PROCESSING");
    }

    /**
     * Process HL7 Validation Queue messages
     */
    @RabbitListener(queues = "#{rabbitMQProperties.queues.hl7Validation}")
    public void processValidationMessage(@Payload String messagePayload,
                                         Message message,
                                         Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        processMessage(messagePayload, message, channel, deliveryTag, "HL7_VALIDATION");
    }

    /**
     * Process HL7 Conversion Queue messages
     */
    @RabbitListener(queues = "#{rabbitMQProperties.queues.hl7Conversion}")
    public void processConversionMessage(@Payload String messagePayload,
                                         Message message,
                                         Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        processMessage(messagePayload, message, channel, deliveryTag, "HL7_CONVERSION");
    }

    /**
     * Process HL7 Storage Queue messages
     */
    @RabbitListener(queues = "#{rabbitMQProperties.queues.hl7Storage}")
    public void processStorageMessage(@Payload String messagePayload,
                                      Message message,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        processMessage(messagePayload, message, channel, deliveryTag, "HL7_STORAGE");
    }

    /**
     * Process Dead Letter Queue messages
     */
    @RabbitListener(queues = "#{rabbitMQProperties.queues.deadLetter}")
    public void processDeadLetterMessage(@Payload String messagePayload,
                                         Message message,
                                         Channel channel,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            QueueMessage queueMessage = objectMapper.readValue(messagePayload, QueueMessage.class);
            log.error("Processing dead letter message - MessageId: {}, CorrelationId: {}, LastError: {}",
                    queueMessage.getMessageId(),
                    queueMessage.getCorrelationId(),
                    queueMessage.getLastError());

            // Handle dead letter message (log, store in database, send alert, etc.)
            handleDeadLetterMessage(queueMessage);

            // Acknowledge the message
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process dead letter message", e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackException) {
                log.error("Failed to NACK dead letter message", nackException);
            }
        }
    }

    /**
     * Generic message processing method
     */
    private void processMessage(String messagePayload, Message message, Channel channel,
                                long deliveryTag, String messageType) {
        QueueMessage queueMessage = null;
        try {
            // Parse the message
            queueMessage = objectMapper.readValue(messagePayload, QueueMessage.class);

            log.info("Processing {} message - MessageId: {}, CorrelationId: {}, Attempt: {}/{}",
                    messageType,
                    queueMessage.getMessageId(),
                    queueMessage.getCorrelationId(),
                    queueMessage.getRetryCount() + 1,
                    queueMessage.getMaxRetries());

            // Process the message based on type
            boolean processed = switch (messageType) {
                case "HL7_PROCESSING" -> processHL7ProcessingMessage(queueMessage);
                case "HL7_VALIDATION" -> processHL7ValidationMessage(queueMessage);
                case "HL7_CONVERSION" -> processHL7ConversionMessage(queueMessage);
                case "HL7_STORAGE" -> processHL7StorageMessage(queueMessage);
                default -> {
                    log.warn("Unknown message type: {}", messageType);
                    yield false;
                }
            };

            if (processed) {
                // Acknowledge successful processing
                channel.basicAck(deliveryTag, false);
                log.info("Successfully processed {} message - MessageId: {}",
                        messageType, queueMessage.getMessageId());
            } else {
                // Handle processing failure
                handleProcessingFailure(queueMessage, channel, deliveryTag,
                        new MessageProcessingException("Message processing returned false"));
            }

        } catch (Exception e) {
            log.error("Error processing {} message - MessageId: {}",
                    messageType, queueMessage != null ? queueMessage.getMessageId() : "unknown", e);

            if (queueMessage != null) {
                handleProcessingFailure(queueMessage, channel, deliveryTag, e);
            } else {
                try {
                    // If we can't parse the message, reject it
                    channel.basicNack(deliveryTag, false, false);
                } catch (Exception nackException) {
                    log.error("Failed to NACK unparseable message", nackException);
                }
            }
        }
    }

    /**
     * Handle processing failure with retry logic
     */
    private void handleProcessingFailure(QueueMessage queueMessage, Channel channel,
                                         long deliveryTag, Exception error) {
        try {
            queueMessage.setLastError(error.getMessage());
            queueMessage.setLastRetryAt(LocalDateTime.now());
            queueMessage.setRetryCount(queueMessage.getRetryCount() + 1);

            if (queueMessage.getRetryCount() < queueMessage.getMaxRetries()) {
                // Retry the message
                log.warn("Retrying message - MessageId: {}, Attempt: {}/{}, Error: {}",
                        queueMessage.getMessageId(),
                        queueMessage.getRetryCount(),
                        queueMessage.getMaxRetries(),
                        error.getMessage());

                // Requeue the message for retry
                channel.basicNack(deliveryTag, false, true);

            } else {
                // Max retries exceeded, send to dead letter queue
                log.error("Max retries exceeded for message - MessageId: {}, sending to dead letter queue",
                        queueMessage.getMessageId());

                queueMessage.setFailedAt(LocalDateTime.now());

                // Send to dead letter queue
                rabbitMQService.sendToQueue(queueMessage, rabbitMQProperties.getQueues().getDeadLetter())
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                success -> log.info("Message sent to dead letter queue: {}", queueMessage.getMessageId()),
                                dlqError -> log.error("Failed to send message to dead letter queue: {}",
                                        queueMessage.getMessageId(), dlqError)
                        );

                // Acknowledge the original message (remove from queue)
                channel.basicAck(deliveryTag, false);
            }

        } catch (Exception e) {
            log.error("Failed to handle processing failure for message: {}",
                    queueMessage.getMessageId(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackException) {
                log.error("Failed to NACK message after handling failure", nackException);
            }
        }
    }

    /**
     * Process HL7 processing messages - implement your business logic here
     */
    private boolean processHL7ProcessingMessage(QueueMessage queueMessage) {
        try {
            log.info("Processing HL7 message - MessageId: {}", queueMessage.getMessageId());

            // Extract payload data
            Map<String, Object> payload = queueMessage.getPayload();
            String organizationId = (String) payload.get("organizationId");
            String hl7MessageType = (String) payload.get("hl7MessageType");
            String patientId = (String) payload.get("patientId");
            String minioPath = (String) payload.get("minioPath");
            String s3Location = (String) payload.get("s3Location");

            // TODO: Implement your HL7 processing logic here
            // 1. Download file from MinIO/S3
            // 2. Parse HL7 message
            // 3. Validate message structure
            // 4. Extract relevant data
            // 5. Store in database
            // 6. Send to next processing stage if needed

            // For now, simulate processing
            Thread.sleep(1000); // Simulate processing time

            log.info("HL7 processing completed - MessageId: {}, OrganizationId: {}, PatientId: {}",
                    queueMessage.getMessageId(), organizationId, patientId);

            return true; // Return true if processing is successful

        } catch (Exception e) {
            log.error("Failed to process HL7 message - MessageId: {}", queueMessage.getMessageId(), e);
            return false;
        }
    }

    /**
     * Process HL7 validation messages
     */
    private boolean processHL7ValidationMessage(QueueMessage queueMessage) {
        try {
            log.info("Validating HL7 message - MessageId: {}", queueMessage.getMessageId());

            // TODO: Implement HL7 validation logic
            // 1. Validate HL7 message structure
            // 2. Check required fields
            // 3. Validate data formats
            // 4. Check business rules

            Thread.sleep(500); // Simulate validation time

            log.info("HL7 validation completed - MessageId: {}", queueMessage.getMessageId());
            return true;

        } catch (Exception e) {
            log.error("Failed to validate HL7 message - MessageId: {}", queueMessage.getMessageId(), e);
            return false;
        }
    }

    /**
     * Process HL7 conversion messages
     */
    private boolean processHL7ConversionMessage(QueueMessage queueMessage) {
        try {
            log.info("Converting HL7 message - MessageId: {}", queueMessage.getMessageId());

            // TODO: Implement HL7 conversion logic
            // 1. Convert HL7 to FHIR or other format
            // 2. Transform data structure
            // 3. Map fields to target schema

            Thread.sleep(800); // Simulate conversion time

            log.info("HL7 conversion completed - MessageId: {}", queueMessage.getMessageId());
            return true;

        } catch (Exception e) {
            log.error("Failed to convert HL7 message - MessageId: {}", queueMessage.getMessageId(), e);
            return false;
        }
    }

    /**
     * Process HL7 storage messages
     */
    private boolean processHL7StorageMessage(QueueMessage queueMessage) {
        try {
            log.info("Storing HL7 message - MessageId: {}", queueMessage.getMessageId());

            // TODO: Implement HL7 storage logic
            // 1. Store processed data in database
            // 2. Index for search
            // 3. Update audit trail
            // 4. Send notifications if needed

            Thread.sleep(300); // Simulate storage time

            log.info("HL7 storage completed - MessageId: {}", queueMessage.getMessageId());
            return true;

        } catch (Exception e) {
            log.error("Failed to store HL7 message - MessageId: {}", queueMessage.getMessageId(), e);
            return false;
        }
    }

    /**
     * Handle dead letter messages
     */
    private void handleDeadLetterMessage(QueueMessage queueMessage) {
        // TODO: Implement dead letter handling logic
        // 1. Log the failure details
        // 2. Store in database for analysis
        // 3. Send alerts to administrators
        // 4. Create support tickets if needed

        log.error("Dead letter message handled - MessageId: {}, LastError: {}",
                queueMessage.getMessageId(), queueMessage.getLastError());
    }
}