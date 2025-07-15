package com.hie.platform.shared.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuditRequest {
    private UUID messageId;
    private UUID correlationId;
    private String serviceName;
    private String status;
    private String stepName;
    private String requestPayload;
    private String responsePayload;
    private String errorMessage;
    private String metadata;
    private Long processingTimeMs;
}