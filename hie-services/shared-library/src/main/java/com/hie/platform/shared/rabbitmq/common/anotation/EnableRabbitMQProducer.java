package com.hie.platform.shared.rabbitmq.common.anotation;

import com.hie.platform.shared.rabbitmq.consumer.config.RabbitMQConsumerConfig;
import com.hie.platform.shared.rabbitmq.producer.config.RabbitMQProducerConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author mismail
 * @description EnableRabbitMQProducer class handles ...
 * @since 24/07/2025
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RabbitMQProducerConfig.class)
public @interface EnableRabbitMQProducer {
}
