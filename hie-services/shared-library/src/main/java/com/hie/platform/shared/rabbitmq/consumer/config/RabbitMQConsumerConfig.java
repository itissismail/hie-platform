package com.hie.platform.shared.rabbitmq.consumer.config;

import com.hie.platform.shared.rabbitmq.common.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rabbitmq.consumer.enabled", havingValue = "true")
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConsumerConfig {

    @Autowired
    private final RabbitMQProperties rabbitMQProperties;

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setDefaultRequeueRejected(false); // Customize as needed
        return factory;
    }


    // Add more listener config beans or dead-letter handling if needed
}