package com.hie.platform.messagerouter;

import com.hie.platform.shared.audit.annotation.EnableAuditTrail;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.hie.platform.messagerouter", "com.hie.platform.shared"})
//@EnableJpaRepositories(basePackages = "com.hie.platform.shared.repository")
@EnableR2dbcRepositories(basePackages = {"com.hie.platform.shared.audit.repository", "com.hie.platform.shared.message.repository"})
@EntityScan(basePackages = "com.hie.platform.shared")
@EnableAuditTrail
public class MessageRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageRouterApplication.class, args);
    }
}

