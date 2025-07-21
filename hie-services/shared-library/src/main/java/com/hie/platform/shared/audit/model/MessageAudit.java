package com.hie.platform.shared.audit.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 *  Author M.Ismail
 *  R2DBC Entity for step-level audit records
 *  Date 15-July-2025
 */

@Table("message_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAudit {

    @Id
    private Long id;

    @Column("message_id")
    private UUID messageId;

    @Column("correlation_id")
    private UUID correlationId;

    @Column("service_name")
    private String serviceName;

    @Column("status")
    private String status;

    @Column("processing_time_ms")
    private Long processingTimeMs;

    @Column("error_message")
    private String errorMessage;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("metadata")
    private String metadata;

    @Column("step_name")
    private String stepName;

    @Column("step_sequence")
    private Integer stepSequence;

    @Column("request_payload")
    private String requestPayload;

    @Column("response_payload")
    private String responsePayload;

    public MessageAudit(UUID messageId, UUID correlationId, String serviceName, String status) {
        this.messageId = messageId;
        this.correlationId = correlationId;
        this.serviceName = serviceName;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public void setMetadataAsJson(Object metadataObject) {
        if (metadataObject == null) {
            this.metadata = null;
        } else if (metadataObject instanceof String) {
            this.metadata = (String) metadataObject;
        } else {
            // Convert object to JSON string
            try {
                ObjectMapper mapper = new ObjectMapper();
                this.metadata = mapper.writeValueAsString(metadataObject);
            } catch (Exception e) {
                this.metadata = "{}";
            }
        }
    }

    public <T> T getMetadataAsObject(Class<T> clazz) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(metadata, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}