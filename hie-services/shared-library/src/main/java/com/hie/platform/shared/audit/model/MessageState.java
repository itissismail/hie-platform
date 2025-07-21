package com.hie.platform.shared.audit.model;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("message_state")
public class MessageState {

    @Id
    private Long id;

    @Column("message_id")
    private UUID messageId;

    @Column("current_status")
    private String currentStatus;

    @Column("source_organization")
    private String sourceOrganization;

    @Column("message_type")
    private String messageType;

    @Column("patient_id")
    private String patientId;

    @Column("global_patient_id")
    private String globalPatientId;

    @Column("s3_location")
    private String s3Location;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_processed_by")
    private String lastProcessedBy;

    @Column("total_processing_time_ms")
    private Long totalProcessingTimeMs;

    @Column("error_message")
    private String errorMessage;
}
