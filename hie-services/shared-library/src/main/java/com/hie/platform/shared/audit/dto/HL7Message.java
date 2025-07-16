package com.hie.platform.shared.audit.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 *  Author M.Ismail
 *  DTO
 *  Date 15-July-2025
 */
public class HL7Message implements Serializable {

    private UUID messageId;
    private UUID correlationId;
    private String rawMessage;
    private String messageType;
    private String sourceOrganization;
    private String patientId;
    private String globalPatientId;
    private String s3Location;
    private LocalDateTime receivedAt;
    private String sendingFacility;
    private String sendingApplication;
    private String receivingFacility;
    private String receivingApplication;
    private String messageControlId;
    private String processingId;
    private String versionId;

    // Constructors
    public HL7Message() {
        this.messageId = UUID.randomUUID();
        this.correlationId = UUID.randomUUID();
        this.receivedAt = LocalDateTime.now();
    }

    public HL7Message(String rawMessage) {
        this();
        this.rawMessage = rawMessage;
    }

    // Getters and setters
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getSourceOrganization() { return sourceOrganization; }
    public void setSourceOrganization(String sourceOrganization) { this.sourceOrganization = sourceOrganization; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getGlobalPatientId() { return globalPatientId; }
    public void setGlobalPatientId(String globalPatientId) { this.globalPatientId = globalPatientId; }

    public String getS3Location() { return s3Location; }
    public void setS3Location(String s3Location) { this.s3Location = s3Location; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public String getSendingFacility() { return sendingFacility; }
    public void setSendingFacility(String sendingFacility) { this.sendingFacility = sendingFacility; }

    public String getSendingApplication() { return sendingApplication; }
    public void setSendingApplication(String sendingApplication) { this.sendingApplication = sendingApplication; }

    public String getReceivingFacility() { return receivingFacility; }
    public void setReceivingFacility(String receivingFacility) { this.receivingFacility = receivingFacility; }

    public String getReceivingApplication() { return receivingApplication; }
    public void setReceivingApplication(String receivingApplication) { this.receivingApplication = receivingApplication; }

    public String getMessageControlId() { return messageControlId; }
    public void setMessageControlId(String messageControlId) { this.messageControlId = messageControlId; }

    public String getProcessingId() { return processingId; }
    public void setProcessingId(String processingId) { this.processingId = processingId; }

    public String getVersionId() { return versionId; }
    public void setVersionId(String versionId) { this.versionId = versionId; }
}