package com.hie.platform.shared.audit.repository;

import com.hie.platform.shared.audit.model.MessageState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageStateRepository extends JpaRepository<MessageState, Long> {

    Optional<MessageState> findByMessageId(UUID messageId);

    List<MessageState> findByCurrentStatusOrderByUpdatedAtDesc(String currentStatus);

    List<MessageState> findBySourceOrganizationOrderByUpdatedAtDesc(String sourceOrganization);

    @Modifying
    @Query("UPDATE MessageState ms SET ms.currentStatus = :status, ms.lastProcessedBy = :serviceName, ms.totalProcessingTimeMs = ms.totalProcessingTimeMs + :additionalTime WHERE ms.messageId = :messageId")
    int updateMessageStatus(@Param("messageId") UUID messageId,
                            @Param("status") String status,
                            @Param("serviceName") String serviceName,
                            @Param("additionalTime") Long additionalTime);
}