package com.hie.platform.shared.rabbitmq.producer.handler;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQErrorHandler implements ErrorHandler, RabbitListenerErrorHandler {

    /**
     * Handle errors that occur during message processing
     */
    @Override
    public void handleError(Throwable t) {
        log.error("RabbitMQ Error Handler - Unhandled error occurred", t);

        // TODO: Implement error handling logic
        // 1. Log error details
        // 2. Send alerts
        // 3. Store error information
        // 4. Metrics collection

        // Determine error type and handle accordingly
        if (t instanceof ListenerExecutionFailedException) {
            handleListenerExecutionException((ListenerExecutionFailedException) t);
        } else {
            handleGenericError(t);
        }
    }

    /**
     * Handle errors in RabbitListener methods
     */

    @Override
    public Object handleError(org.springframework.amqp.core.Message message, Channel channel, Message<?> message1, ListenerExecutionFailedException e) throws Exception {
        log.error("RabbitListener Error Handler - Message: {} and error ",
                message.toString(), e);

        // TODO: Implement specific error handling for listener methods
        // 1. Extract message information
        // 2. Log error context
        // 3. Return appropriate response or null

        return null;
    }

    /**
     * Handle ListenerExecutionFailedException specifically
     */
    private void handleListenerExecutionException(ListenerExecutionFailedException exception) {
        log.error("Listener Execution Failed - Queue: {}, Consumer: {}, Message: {}",
                exception.getFailedMessage() != null ? exception.getFailedMessage().getMessageProperties().getConsumerQueue() : "unknown",
                exception.getFailedMessage() != null ? exception.getFailedMessage().getMessageProperties().getConsumerTag() : "unknown",
                exception.getFailedMessage() != null ? new String(exception.getFailedMessage().getBody()) : "unknown",
                exception);

        // TODO: Add specific handling for listener execution failures
        // 1. Check if message should be retried
        // 2. Send to dead letter queue if appropriate
        // 3. Update metrics
    }

    /**
     * Handle generic errors
     */
    private void handleGenericError(Throwable t) {
        log.error("Generic RabbitMQ Error occurred: {}", t.getMessage(), t);

        // TODO: Add generic error handling
        // 1. Classification of error types
        // 2. Appropriate response based on error type
        // 3. Alerting and monitoring
    }


}