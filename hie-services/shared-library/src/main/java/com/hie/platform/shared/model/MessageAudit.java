package com.hie.platform.shared.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAudit {

    public MessageAudit(UUID messageId, UUID correlationId, String serviceName, String status) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.serviceName = serviceName;
        this.status = status;
    }

    public MessageAudit(UUID messageId, UUID correlationId, String serviceName, String status, Long processingTimeMs, String errorMessage, LocalDateTime createdAt, String metadata, String stepName, Integer stepSequence, String requestPayload, String responsePayload) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.serviceName = serviceName;
        this.status = status;
        this.processingTimeMs = processingTimeMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.metadata = metadata;
        this.stepName = stepName;
        this.stepSequence = stepSequence;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /*    @Column(name = "metadata", columnDefinition = "jsonb")
        @Type(value = "io.hypersistence.utils.hibernate.type.json.JsonType")
        private String metadata;*/
    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "step_sequence")
    private Integer stepSequence;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}