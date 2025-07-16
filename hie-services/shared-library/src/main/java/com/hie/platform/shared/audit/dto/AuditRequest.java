package com.hie.platform.shared.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 *  Author M.Ismail
 *  DTO
 *  Date 15-July-2025
 */

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