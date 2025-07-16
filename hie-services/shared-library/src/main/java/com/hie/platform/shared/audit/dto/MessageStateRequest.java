package com.hie.platform.shared.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 *  Author M.Ismail
 *  DTO for service operations
 *  Date 15-July-2025
 */
@Data
@Builder
public class MessageStateRequest {
    private UUID messageId;
    private String currentStatus;
    private String sourceOrganization;
    private String messageType;
    private String patientId;
    private String globalPatientId;
    private String s3Location;
    private String lastProcessedBy;
    private Long additionalProcessingTime;
}
