package com.hie.platform.shared.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private UUID messageId;

    @Column(name = "current_status", nullable = false, length = 20)
    private String currentStatus;

    @Column(name = "source_organization", nullable = false, length = 100)
    private String sourceOrganization;

    @Column(name = "message_type", nullable = false, length = 10)
    private String messageType;

    @Column(name = "patient_id", length = 50)
    private String patientId;

    @Column(name = "global_patient_id", length = 50)
    private String globalPatientId;

    @Column(name = "s3_location", length = 500)
    private String s3Location;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_processed_by", length = 50)
    private String lastProcessedBy;

    @Column(name = "total_processing_time_ms")
    private Long totalProcessingTimeMs;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
