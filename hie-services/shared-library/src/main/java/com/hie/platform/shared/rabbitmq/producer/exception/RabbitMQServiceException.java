package com.hie.platform.shared.rabbitmq.producer.exception;

public class RabbitMQServiceException extends RuntimeException {
    public RabbitMQServiceException(String message) {
        super(message);
    }

    public RabbitMQServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}