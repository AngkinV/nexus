package com.nexus.chat.repository;

import com.nexus.chat.model.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {
    
    Optional<MessageReadStatus> findByMessageIdAndUserId(Long messageId, Long userId);
    
    List<MessageReadStatus> findByMessageId(Long messageId);
    
    Long countByUserIdAndIsReadFalse(Long userId);
    
}
