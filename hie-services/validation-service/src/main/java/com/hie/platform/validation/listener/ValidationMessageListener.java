package com.hie.platform.validation.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.shared.rabbitmq.consumer.service.RabbitMQConsumerService;
import com.hie.platform.shared.rabbitmq.producer.service.RabbitMQService;
import com.hie.platform.validation.service.HL7ValidationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationMessageListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queues.hl7-processing}")
    public void receiveRawMessage(QueueMessage message) {
        try {
            //QueueMessage message = objectMapper.readValue(jsonMessage, QueueMessage.class);
            log.info("Received HL7 message for validation: {}", message.getMessageId());

            // Perform validation logic
            // Example: check required fields, structure, etc.

            // Save results to DB
            // Update state
            // Forward to next queue (via RabbitMQService, if needed)

        } catch (Exception e) {
            log.error("Error processing HL7 message", e);
            // Handle DLQ, retries, or error state
        }
    }
}