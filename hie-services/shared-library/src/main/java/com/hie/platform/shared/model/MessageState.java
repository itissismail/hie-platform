package com.hie.platform.shared.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_state")
public class MessageState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private UUID messageId;

    @Column(name = "current_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MessageStatus currentStatus;

    @Column(name = "source_organization", nullable = false)
    private String sourceOrganization;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "global_patient_id")
    private String globalPatientId;

    @Column(name = "s3_location")
    private String s3Location;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public MessageState() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public MessageStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(MessageStatus currentStatus) {
        this.currentStatus = currentStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSourceOrganization() { return sourceOrganization; }
    public void setSourceOrganization(String sourceOrganization) { this.sourceOrganization = sourceOrganization; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getGlobalPatientId() { return globalPatientId; }
    public void setGlobalPatientId(String globalPatientId) { this.globalPatientId = globalPatientId; }

    public String getS3Location() { return s3Location; }
    public void setS3Location(String s3Location) { this.s3Location = s3Location; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}