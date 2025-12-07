package com.nexus.chat.repository;

import com.nexus.chat.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByChatIdOrderByCreatedAtDesc(Long chatId, Pageable pageable);

    List<Message> findByChatIdOrderByCreatedAtDesc(Long chatId);

    Long countByChatId(Long chatId);

    /**
     * Count messages sent by a user
     */
    long countBySenderId(Long senderId);

}
