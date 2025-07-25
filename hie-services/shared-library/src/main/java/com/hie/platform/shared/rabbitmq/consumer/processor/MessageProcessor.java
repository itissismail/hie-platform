package com.hie.platform.shared.rabbitmq.consumer.processor;



import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;

/**
 * @author mismail
 * @description MessageProcessor class handles ...
 * @since 25/07/2025
 */

/**
 * Interface for processing queue messages
 * Implement this interface in your service to define custom message processing logic
 */
public interface MessageProcessor {

    /**
     * Process a queue message
     * @param queueMessage The message to process
     * @param messageContent The actual content retrieved from MinIO (if applicable)
     * @return ProcessingResult containing success status and optional next queue information
     */
    ProcessingResult processMessage(QueueMessage queueMessage, String messageContent);

    /**
     * Get the processor name for logging purposes
     * @return processor name
     */
    default String getProcessorName() {
        return this.getClass().getSimpleName();
    }
}
