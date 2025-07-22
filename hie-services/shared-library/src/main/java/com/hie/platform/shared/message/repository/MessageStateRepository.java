
package com.hie.platform.shared.message.repository;

import com.hie.platform.shared.audit.model.MessageState;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MessageStateRepository extends R2dbcRepository<MessageState, Long> {

    Mono<MessageState> findByMessageId(UUID messageId);

    Flux<MessageState> findByCurrentStatusOrderByUpdatedAtDesc(String currentStatus);

    Flux<MessageState> findBySourceOrganizationOrderByUpdatedAtDesc(String sourceOrganization);

    @Modifying
    @Query("UPDATE message_state SET current_status = :status, last_processed_by = :serviceName, " +
            "total_processing_time_ms = total_processing_time_ms + :additionalTime, updated_at = NOW() " +
            "WHERE message_id = :messageId")
    Mono<Integer> updateMessageStatus(@Param("messageId") UUID messageId,
                                      @Param("status") String status,
                                      @Param("serviceName") String serviceName,
                                      @Param("additionalTime") Long additionalTime);

    @Modifying
    @Query("UPDATE message_state SET current_status = :status, last_processed_by = :serviceName, " +
            "updated_at = NOW() WHERE message_id = :messageId")
    Mono<Integer> updateMessageStatus(@Param("messageId") UUID messageId,
                                      @Param("status") String status,
                                      @Param("serviceName") String serviceName);

    @Query("SELECT * FROM message_state WHERE patient_id = :patientId ORDER BY updated_at DESC")
    Flux<MessageState> findByPatientIdOrderByUpdatedAtDesc(@Param("patientId") String patientId);

    @Query("SELECT * FROM message_state WHERE current_status = :status AND source_organization = :sourceOrg ORDER BY updated_at DESC")
    Flux<MessageState> findByStatusAndSourceOrganization(@Param("status") String status,
                                                         @Param("sourceOrg") String sourceOrganization);
}