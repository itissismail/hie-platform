package com.hie.platform.validation;

import com.hie.platform.shared.audit.annotation.EnableAuditTrail;
import com.hie.platform.shared.rabbitmq.common.anotation.EnableRabbitMQConsumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.hie.platform.validation",         // or message-router depending on the service
        "com.hie.platform.shared"              // shared library
})
/*@EnableR2dbcRepositories(basePackages = {
        "com.hie.platform.shared.message.repository",
        "com.hie.platform.shared.audit.repository"
})*/
@EntityScan(basePackages = "com.hie.platform.shared")
@EnableAuditTrail

public class ValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationServiceApplication.class, args);
    }
}