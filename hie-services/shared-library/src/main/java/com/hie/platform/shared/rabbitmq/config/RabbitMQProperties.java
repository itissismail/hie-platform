package com.hie.platform.shared.rabbitmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {
    private String host;
    private int port;
    private String username;
    private String password;
    private String virtualHost;
    private String exchangeName;
    private String serviceName;
    private QueueNames queues = new QueueNames();
    private RoutingKeys routingKeys = new RoutingKeys();
    private MessageSettings messageSettings = new MessageSettings();

    @Data
    public static class QueueNames {
        private String hl7Processing;
        private String hl7Validation;
        private String hl7Conversion;
        private String hl7Storage;
        private String deadLetter;
    }

    @Data
    public static class RoutingKeys {
        private String hl7Processing;
        private String hl7Validation;
        private String hl7Conversion;
        private String hl7Storage;
    }

    @Data
    public static class MessageSettings {
        private long messageTtl = 3600000; // 1 hour default
        private int maxRetries = 3;
        private boolean durable = true;
        private boolean persistent = true;
    }
}