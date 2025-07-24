package com.hie.platform.shared.rabbitmq.common;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author mismail
 * @description RabbitMQBaseConfig class handles ...
 * @since 24/07/2025
 */
@Configuration
public class RabbitMQBaseConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
