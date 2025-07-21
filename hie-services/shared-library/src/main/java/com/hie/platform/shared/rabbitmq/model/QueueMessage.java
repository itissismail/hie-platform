package com.hie.platform.shared.rabbitmq.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class QueueMessage {
    private String messageId;
    private String correlationId;
    private String messageType;
    private Map<String, Object> payload;
    private Integer priority;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String sentBy;
    private Map<String, String> headers;
}