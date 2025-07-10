package com.hie.platform.messagerouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.hie.platform.messagerouter", "com.hie.platform.shared"})
@EnableJpaRepositories(basePackages = "com.hie.platform.shared.repository")
@EntityScan(basePackages = "com.hie.platform.shared")
public class MessageRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageRouterApplication.class, args);
    }
}

