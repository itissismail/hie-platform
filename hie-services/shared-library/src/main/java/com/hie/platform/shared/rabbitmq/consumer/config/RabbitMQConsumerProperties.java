package com.hie.platform.shared.rabbitmq.consumer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@Component// Explicitly name the bean
@ConfigurationProperties(prefix = "rabbitmq")

public class RabbitMQConsumerProperties {
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
    private Consumer consumer = new Consumer(); // New consumer settings

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

    @Data
    public static class Consumer {
        private int concurrentConsumers = 1;
        private int maxConcurrentConsumers = 5;
        private int prefetchCount = 10;
        private boolean autoStartup = true;
        private long receiveTimeout = 30000; // 30 seconds
        private long shutdownTimeout = 5000; // 5 seconds
        private boolean defaultRequeueRejected = false;

        // Retry configuration for consumers
        private RetryConfig retry = new RetryConfig();
    }

    @Data
    public static class RetryConfig {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private long initialInterval = 1000; // 1 second
        private double multiplier = 2.0;
        private long maxInterval = 10000; // 10 seconds
    }
}