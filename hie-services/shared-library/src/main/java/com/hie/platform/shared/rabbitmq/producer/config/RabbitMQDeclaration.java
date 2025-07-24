package com.hie.platform.shared.rabbitmq.producer.config;

import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RabbitMQDeclaration {

    private final RabbitMQProperties properties;

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        // Ensure auto-startup so declarations happen
        admin.setAutoStartup(true);
        log.info("RabbitAdmin configured with auto-startup enabled");
        return admin;
    }

    @Bean
    public DirectExchange hl7Exchange() {
        DirectExchange exchange = ExchangeBuilder
                .directExchange(properties.getExchangeName())
                .durable(true)  // CRITICAL: Exchange must be durable
                .build();
        log.info("Created durable exchange: {}", properties.getExchangeName());
        return exchange;
    }

    @Bean
    public Queue hl7ProcessingQueue() {
        Map<String, Object> args = new HashMap<>();

        // Set message TTL if configured
        if (properties.getMessageSettings().getMessageTtl() > 0) {
            args.put("x-message-ttl", properties.getMessageSettings().getMessageTtl());
        }

        // Configure dead letter exchange for failed messages
        if (properties.getQueues().getDeadLetter() != null) {
            args.put("x-dead-letter-exchange", properties.getExchangeName());
            args.put("x-dead-letter-routing-key", "dead-letter");
        }

        Queue queue = QueueBuilder
                .durable(properties.getQueues().getHl7Processing())  // CRITICAL: Queue must be durable
                .withArguments(args)
                .build();

        log.info("Created durable queue: {} with args: {}",
                properties.getQueues().getHl7Processing(), args);
        return queue;
    }

    @Bean
    public Queue hl7ValidationQueue() {
        Queue queue = QueueBuilder
                .durable(properties.getQueues().getHl7Validation())
                .build();
        log.info("Created durable validation queue: {}", properties.getQueues().getHl7Validation());
        return queue;
    }

    @Bean
    public Queue hl7ConversionQueue() {
        Queue queue = QueueBuilder
                .durable(properties.getQueues().getHl7Conversion())
                .build();
        log.info("Created durable conversion queue: {}", properties.getQueues().getHl7Conversion());
        return queue;
    }

    @Bean
    public Queue hl7StorageQueue() {
        Queue queue = QueueBuilder
                .durable(properties.getQueues().getHl7Storage())
                .build();
        log.info("Created durable storage queue: {}", properties.getQueues().getHl7Storage());
        return queue;
    }

    @Bean
    public Queue deadLetterQueue() {
        Queue queue = QueueBuilder
                .durable(properties.getQueues().getDeadLetter())
                .build();
        log.info("Created dead letter queue: {}", properties.getQueues().getDeadLetter());
        return queue;
    }

    // Bindings
    @Bean
    public Binding hl7ProcessingBinding() {
        Binding binding = BindingBuilder
                .bind(hl7ProcessingQueue())
                .to(hl7Exchange())
                .with(properties.getRoutingKeys().getHl7Processing());
        log.info("Created binding: queue={}, exchange={}, routingKey={}",
                properties.getQueues().getHl7Processing(),
                properties.getExchangeName(),
                properties.getRoutingKeys().getHl7Processing());
        return binding;
    }

    @Bean
    public Binding hl7ValidationBinding() {
        return BindingBuilder
                .bind(hl7ValidationQueue())
                .to(hl7Exchange())
                .with(properties.getRoutingKeys().getHl7Validation());
    }

    @Bean
    public Binding hl7ConversionBinding() {
        return BindingBuilder
                .bind(hl7ConversionQueue())
                .to(hl7Exchange())
                .with(properties.getRoutingKeys().getHl7Conversion());
    }

    @Bean
    public Binding hl7StorageBinding() {
        return BindingBuilder
                .bind(hl7StorageQueue())
                .to(hl7Exchange())
                .with(properties.getRoutingKeys().getHl7Storage());
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(hl7Exchange())
                .with("dead-letter");
    }
}