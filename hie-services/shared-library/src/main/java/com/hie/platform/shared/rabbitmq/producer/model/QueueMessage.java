package com.hie.platform.shared.rabbitmq.producer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String lastError;
    private LocalDateTime lastRetryAt;
    private LocalDateTime failedAt;
    private Map<String, Object> customHeaders;


}