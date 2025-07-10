package com.hie.platform.shared.service;

import com.hie.platform.shared.dto.HL7Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessagePublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishToQueue(String queueName, HL7Message message) {
        rabbitTemplate.convertAndSend(queueName, message);
    }

    public void publishToValidationQueue(HL7Message message) {
        publishToQueue("validation-queue", message);
    }

    public void publishToIntakeQueue(HL7Message message) {
        publishToQueue("intake-queue", message);
    }

    public void publishToConversionQueue(HL7Message message) {
        publishToQueue("conversion-queue", message);
    }

    public void publishToStorageQueue(HL7Message message) {
        publishToQueue("storage-queue", message);
    }
}
