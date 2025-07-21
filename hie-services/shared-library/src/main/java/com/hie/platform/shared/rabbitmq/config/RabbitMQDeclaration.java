package com.hie.platform.shared.rabbitmq.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQDeclaration {

    private final RabbitMQProperties properties;

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DirectExchange hl7Exchange() {
        return ExchangeBuilder
                .directExchange(properties.getExchangeName())
                .durable(true)
                .build();
    }

    @Bean
    public Queue hl7ProcessingQueue() {
        return QueueBuilder
                .durable(properties.getQueues().getHl7Processing())
                .build();
    }

    @Bean
    public Binding hl7ProcessingBinding() {
        return BindingBuilder
                .bind(hl7ProcessingQueue())
                .to(hl7Exchange())
                .with(properties.getRoutingKeys().getHl7Processing());
    }

    // Repeat similar beans if you want to auto-declare other queues/bindings
}
