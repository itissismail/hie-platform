package com.hie.platform.shared.rabbitmq.config;

import com.hie.platform.shared.rabbitmq.config.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitMQProperties rabbitMQProperties;

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setMandatory(true); // Enable returns for unrouted messages

        // Log any returned messages
        template.setReturnsCallback(returnedMessage -> {
            System.err.println("Message returned: " + returnedMessage.getMessage());
            System.err.println("Reply code: " + returnedMessage.getReplyCode());
            System.err.println("Reply text: " + returnedMessage.getReplyText());
            System.err.println("Exchange: " + returnedMessage.getExchange());
            System.err.println("Routing key: " + returnedMessage.getRoutingKey());
        });

        // Set default delivery mode using a MessagePostProcessor
        if (rabbitMQProperties.getMessageSettings().isPersistent()) {
            template.setBeforePublishPostProcessors(message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            });
        }

        return template;
    }
}
