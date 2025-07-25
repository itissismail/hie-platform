package com.hie.platform.shared.rabbitmq.consumer.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.minio.service.MinioService;
import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.consumer.processor.MessageProcessor;
import com.hie.platform.shared.rabbitmq.consumer.processor.ProcessingResult;
import com.hie.platform.shared.rabbitmq.producer.exception.MessageProcessingException;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.producer.service.RabbitMQService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract helper class for consuming RabbitMQ messages with common retry/DLQ logic
 * Services can extend this class and implement the message processing logic
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractMessageConsumer {

    protected final ObjectMapper objectMapper;
    protected final RabbitMQProperties rabbitMQProperties;
    protected final MinioService minioService;
    private final RabbitMQService rabbitMQService;

    /**
     * Get the message processor implementation
     * @return MessageProcessor instance
     */
    protected abstract MessageProcessor getMessageProcessor();

    /**
     * Process message with built-in retry and error handling
     */
    protected void processMessage(@Payload String messagePayload,
                                  Message message,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  String processorType) {
        QueueMessage queueMessage = null;
        try {
            // Parse the message
            queueMessage = objectMapper.readValue(messagePayload, QueueMessage.class);

            log.info("Processing {} message - MessageId: {}, CorrelationId: {}, Attempt: {}/{}",
                    processorType,
                    queueMessage.getMessageId(),
                    queueMessage.getCorrelationId(),
                    queueMessage.getRetryCount() + 1,
                    queueMessage.getMaxRetries());

            // Retrieve message content from MinIO if path exists
            String messageContent = retrieveMessageContent(queueMessage);

            // Process the message using the provided processor
            MessageProcessor processor = getMessageProcessor();
            ProcessingResult result = processor.processMessage(queueMessage, messageContent);

            if (result.isSuccess()) {
                // Handle successful processing
                handleSuccessfulProcessing(queueMessage, result, channel, deliveryTag, processorType);
            } else {
                // Handle processing failure
                handleProcessingFailure(queueMessage, channel, deliveryTag,
                        new MessageProcessingException(result.getErrorMessage()));
            }

        } catch (Exception e) {
            log.error("Error processing {} message - MessageId: {}",
                    processorType, queueMessage != null ? queueMessage.getMessageId() : "unknown", e);

            if (queueMessage != null) {
                handleProcessingFailure(queueMessage, channel, deliveryTag, e);
            } else {
                try {
                    // If we can't parse the message, reject it
                    channel.basicNack(deliveryTag, false, false);
                    log.warn("Rejected unparseable message");
                } catch (Exception nackException) {
                    log.error("Failed to NACK unparseable message", nackException);
                }
            }
        }
    }

    /**
     * Retrieve message content from MinIO
     */
    private String retrieveMessageContent(QueueMessage queueMessage) {
        try {
            Map<String, Object> payload = queueMessage.getPayload();
            String minioPath = (String) payload.get("minioPath");

            if (minioPath == null || minioPath.isEmpty()) {
                log.debug("No MinIO path found in message payload - MessageId: {}", queueMessage.getMessageId());
                return null;
            }

            log.debug("Retrieving message content from MinIO path: {} - MessageId: {}",
                    minioPath, queueMessage.getMessageId());

            String content = minioService.downloadFileAsStringSync(minioPath);

            if (content == null || content.isEmpty()) {
                log.warn("Retrieved empty content from MinIO path: {} - MessageId: {}",
                        minioPath, queueMessage.getMessageId());
            } else {
                log.debug("Successfully retrieved content from MinIO - Size: {} bytes - MessageId: {}",
                        content.length(), queueMessage.getMessageId());
            }

            return content;

        } catch (Exception e) {
            log.error("Failed to retrieve message content from MinIO - MessageId: {}",
                    queueMessage.getMessageId(), e);
            throw new MessageProcessingException("Failed to retrieve message content from MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Handle successful message processing
     */
    private void handleSuccessfulProcessing(QueueMessage queueMessage,
                                            ProcessingResult result,
                                            Channel channel,
                                            long deliveryTag,
                                            String processorType) throws Exception {
        // Acknowledge the message
        channel.basicAck(deliveryTag, false);
        log.info("Successfully processed {} message - MessageId: {}",
                processorType, queueMessage.getMessageId());

        // Forward to next queue if specified
        if (result.getNextQueue() != null && !result.getNextQueue().isEmpty()) {
            forwardToNextQueue(queueMessage, result);
        }
    }

    /**
     * Forward message to next queue
     */
    private void forwardToNextQueue(QueueMessage queueMessage, ProcessingResult result) {
        try {
            // Update message metadata if needed
            if (result.getProcessedData() != null) {
                // Add processed data to payload or update existing data
                queueMessage.getPayload().put("processedData", result.getProcessedData());
            }

            // Reset retry count for next stage
            queueMessage.setRetryCount(0);
            queueMessage.setLastError(null);
            queueMessage.setLastRetryAt(null);

            // Send to next queue asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    rabbitMQService.sendToQueue(queueMessage, result.getNextQueue())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    success -> log.info("Message forwarded to next queue: {} - MessageId: {}",
                                            result.getNextQueue(), queueMessage.getMessageId()),
                                    error -> log.error("Failed to forward message to next queue: {} - MessageId: {}",
                                            result.getNextQueue(), queueMessage.getMessageId(), error)
                            );
                } catch (Exception e) {
                    log.error("Error forwarding message to next queue: {} - MessageId: {}",
                            result.getNextQueue(), queueMessage.getMessageId(), e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to forward message to next queue: {} - MessageId: {}",
                    result.getNextQueue(), queueMessage.getMessageId(), e);
        }
    }

    /**
     * Handle processing failure with retry logic
     */
    private void handleProcessingFailure(QueueMessage queueMessage,
                                         Channel channel,
                                         long deliveryTag,
                                         Exception error) {
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

                // Calculate retry delay (exponential backoff)
                long retryDelay = calculateRetryDelay(queueMessage.getRetryCount());

                // For simplicity, we'll requeue immediately
                // In production, you might want to implement delayed retry using RabbitMQ delayed message plugin
                channel.basicNack(deliveryTag, false, true);

            } else {
                // Max retries exceeded, send to dead letter queue
                log.error("Max retries exceeded for message - MessageId: {}, sending to dead letter queue",
                        queueMessage.getMessageId());

                queueMessage.setFailedAt(LocalDateTime.now());

                // Send to dead letter queue asynchronously
                CompletableFuture.runAsync(() -> {
                    try {
                        rabbitMQService.sendToQueue(queueMessage, rabbitMQProperties.getQueues().getDeadLetter())
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe(
                                        success -> log.info("Message sent to dead letter queue: {}", queueMessage.getMessageId()),
                                        dlqError -> log.error("Failed to send message to dead letter queue: {}",
                                                queueMessage.getMessageId(), dlqError)
                                );
                    } catch (Exception e) {
                        log.error("Error sending message to DLQ: {}", queueMessage.getMessageId(), e);
                    }
                });

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
     * Calculate retry delay using exponential backoff
     */
    private long calculateRetryDelay(int retryCount) {
        // Base delay of 1 second, exponential backoff with max of 60 seconds
        long baseDelay = 1000; // 1 second
        long maxDelay = 60000; // 60 seconds
        long delay = (long) (baseDelay * Math.pow(2, retryCount - 1));
        return Math.min(delay, maxDelay);
    }

    /**
     * Create a dead letter message for manual inspection
     */
    protected void handleDeadLetterMessage(QueueMessage queueMessage) {
        log.error("Processing dead letter message - MessageId: {}, CorrelationId: {}, LastError: {}",
                queueMessage.getMessageId(),
                queueMessage.getCorrelationId(),
                queueMessage.getLastError());

        // Implement custom dead letter handling logic here
        // e.g., store in database, send alerts, create support tickets

        // For now, just log the details
        log.info("Dead letter message details - OrganizationId: {}, RetryCount: {}, FailedAt: {}",
                queueMessage.getPayload().get("organizationId"),
                queueMessage.getRetryCount(),
                queueMessage.getFailedAt());
    }
}