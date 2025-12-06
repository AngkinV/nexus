package com.nexus.chat.repository;

import com.nexus.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    
    @Query("SELECT c FROM Chat c JOIN ChatMember cm ON c.id = cm.chatId WHERE cm.userId = :userId ORDER BY c.lastMessageAt DESC")
    List<Chat> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);
    
}
