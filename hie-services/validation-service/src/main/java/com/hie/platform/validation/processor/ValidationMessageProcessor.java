package com.hie.platform.validation.processor;

import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import com.hie.platform.shared.rabbitmq.consumer.config.RabbitMQConsumerConfig;
import com.hie.platform.shared.rabbitmq.consumer.config.RabbitMQConsumerProperties;
import com.hie.platform.shared.rabbitmq.consumer.processor.MessageProcessor;
import com.hie.platform.shared.rabbitmq.consumer.processor.ProcessingResult;
import com.hie.platform.shared.rabbitmq.producer.model.QueueMessage;
import com.hie.platform.validation.service.HL7ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Message processor implementation for HL7 validation
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ValidationMessageProcessor implements MessageProcessor {

   /* private final ObjectMapper objectMapper;

   @RabbitListener(queues = "${rabbitmq.queues.hl7-processing}")
    //@AuditStep(serviceName = "validation-service", stepName = MessageStatus.VALIDATED)
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
    }*/


    private final HL7ValidationService hl7ValidationService;
    private final RabbitMQConsumerConfig rabbitMQConsumerConfig;

    @Override
    @RabbitListener(queues = "${rabbitmq.queues.hl7-processing}")
    public ProcessingResult processMessage(QueueMessage queueMessage) {
        try {
            String messageContent="";
            log.info("Starting HL7 validation - MessageId: {}", queueMessage.getMessageId());

            // Extract metadata from payload
            Map<String, Object> payload = queueMessage.getPayload();
            String organizationId = (String) payload.get("organizationId");
            String hl7MessageType = (String) payload.get("hl7MessageType");
            String patientId = (String) payload.get("patientId");
            messageContent = (String) payload.get("s3Location");
            log.debug("Processing HL7 validation - OrganizationId: {}, MessageType: {}, PatientId: {}",
                    organizationId, hl7MessageType, patientId);

            // Validate that we have message content
            if (messageContent == null || messageContent.trim().isEmpty()) {
                return ProcessingResult.failure("Message content is empty or null");
            }

            // Perform HL7 validation using the validation service
            boolean validationSuccess = hl7ValidationService.validateHL7Message(queueMessage);

            if (validationSuccess) {
                log.info("HL7 validation successful - MessageId: {}", queueMessage.getMessageId());

                // Return success with next queue for further processing
                // Forward to conversion queue after successful validation
                String nextQueue = rabbitMQConsumerConfig.getConsumerProperties().getQueues().getHl7Conversion();
                return ProcessingResult.success(nextQueue);
            } else {
                log.error("HL7 validation failed - MessageId: {}", queueMessage.getMessageId());
                return ProcessingResult.failure("HL7 validation failed - check logs for details");
            }

        } catch (Exception e) {
            log.error("Exception during HL7 validation processing - MessageId: {}",
                    queueMessage.getMessageId(), e);
            return ProcessingResult.failure("Validation processing failed", e);
        }

    }
}