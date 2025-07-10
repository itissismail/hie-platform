package com.hie.platform.shared.repository;

import com.hie.platform.shared.model.MessageState;
import com.hie.platform.shared.model.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageStateRepository extends JpaRepository<MessageState, Long> {
    Optional<MessageState> findByMessageId(UUID messageId);
    List<MessageState> findByCurrentStatus(MessageStatus status);
    List<MessageState> findBySourceOrganization(String sourceOrganization);
}