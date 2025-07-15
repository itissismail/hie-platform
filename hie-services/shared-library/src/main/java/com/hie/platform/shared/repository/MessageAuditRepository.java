package com.hie.platform.shared.repository;

import com.hie.platform.shared.model.MessageAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageAuditRepository extends JpaRepository<MessageAudit, Long> {
    List<MessageAudit> findByMessageIdOrderByStepSequenceAsc(UUID messageId);

    List<MessageAudit> findByCorrelationIdOrderByCreatedAtAsc(UUID correlationId);

    List<MessageAudit> findByServiceNameAndStatusOrderByCreatedAtDesc(String serviceName, String status);

    @Query("SELECT MAX(ma.stepSequence) FROM MessageAudit ma WHERE ma.messageId = :messageId")
    Integer findMaxStepSequence(@Param("messageId") UUID messageId);
}