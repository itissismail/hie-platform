package com.hie.platform.messagerouter.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author mismail
 * @description RabbitMQConfiguration class handles ...
 * @since 21/07/2025
 */

//@Configuration
//@Slf4j
public class RabbitMQConfiguration {

  /*  @Value("${rabbitmq.exchange.name:hl7-processing-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.name:hl7-processing-queue}")
    private String queueName;

    @Value("${rabbitmq.routing.key:hl7.process}")
    private String routingKey;

    @Value("${rabbitmq.dlq.name:hl7-processing-dlq}")
    private String deadLetterQueue;

    @Value("${rabbitmq.dlx.name:hl7-processing-dlx}")
    private String deadLetterExchange;

    // Create the main exchange for HL7 message processing
    @Bean
    public TopicExchange hl7ProcessingExchange() {
        log.info("Creating HL7 processing exchange: {}", exchangeName);
        return ExchangeBuilder
                .topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    // Create the main processing queue with dead letter configuration
    @Bean
    public Queue hl7ProcessingQueue() {
        log.info("Creating HL7 processing queue: {}", queueName);
        return QueueBuilder
                .durable(queueName)
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", "hl7.failed")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    // Create dead letter exchange for failed messages
    @Bean
    public TopicExchange deadLetterExchange() {
        log.info("Creating dead letter exchange: {}", deadLetterExchange);
        return ExchangeBuilder
                .topicExchange(deadLetterExchange)
                .durable(true)
                .build();
    }

    // Create dead letter queue
    @Bean
    public Queue deadLetterQueue() {
        log.info("Creating dead letter queue: {}", deadLetterQueue);
        return QueueBuilder
                .durable(deadLetterQueue)
                .build();
    }

    // Bind main queue to main exchange
    @Bean
    public Binding hl7ProcessingBinding() {
        log.info("Binding queue {} to exchange {} with routing key {}", queueName, exchangeName, routingKey);
        return BindingBuilder
                .bind(hl7ProcessingQueue())
                .to(hl7ProcessingExchange())
                .with(routingKey);
    }

    // Bind dead letter queue to dead letter exchange
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("hl7.failed");
    }

    // Configure RabbitTemplate with JSON converter
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());

        // Optional: Set default exchange and routing key
        template.setExchange(exchangeName);
        template.setRoutingKey(routingKey);

        log.info("RabbitTemplate configured with exchange: {} and routing key: {}", exchangeName, routingKey);
        return template;
    }

    // ObjectMapper bean for JSON serialization
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Handle LocalDateTime serialization
        return mapper;
    }*/
}