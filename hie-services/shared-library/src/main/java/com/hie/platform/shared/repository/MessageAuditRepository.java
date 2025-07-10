package com.hie.platform.shared.repository;

import com.hie.platform.shared.model.MessageAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageAuditRepository extends JpaRepository<MessageAudit, Long> {
    List<MessageAudit> findByCorrelationIdOrderByCreatedAtDesc(UUID correlationId);
    List<MessageAudit> findByMessageIdOrderByCreatedAtDesc(UUID messageId);
    List<MessageAudit> findByServiceNameOrderByCreatedAtDesc(String serviceName);
}