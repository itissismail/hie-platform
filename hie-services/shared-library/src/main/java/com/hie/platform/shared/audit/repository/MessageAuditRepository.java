package com.hie.platform.shared.audit.repository;

import com.hie.platform.shared.audit.model.MessageAudit;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface MessageAuditRepository extends R2dbcRepository<MessageAudit, Long> {

    Flux<MessageAudit> findByMessageIdOrderByStepSequenceAsc(UUID messageId);

    Flux<MessageAudit> findByCorrelationIdOrderByCreatedAtAsc(UUID correlationId);

    Flux<MessageAudit> findByServiceNameAndStatusOrderByCreatedAtDesc(String serviceName, String status);

    @Query("SELECT MAX(step_sequence) FROM message_audit WHERE message_id = :messageId")
    Mono<Integer> findMaxStepSequence(@Param("messageId") UUID messageId);

    @Query("SELECT COUNT(*) FROM message_audit WHERE message_id = :messageId AND status = 'FAILED'")
    Mono<Long> countFailedStepsByMessageId(@Param("messageId") UUID messageId);

    @Query("SELECT * FROM message_audit WHERE message_id = :messageId AND step_name = :stepName ORDER BY created_at DESC LIMIT 1")
    Mono<MessageAudit> findLatestStepByMessageIdAndStepName(@Param("messageId") UUID messageId, @Param("stepName") String stepName);
}