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

    /**
     * Find all group chats for a user
     */
    @Query("SELECT c FROM Chat c JOIN ChatMember cm ON c.id = cm.chatId WHERE cm.userId = :userId AND c.type = 'group' ORDER BY c.lastMessageAt DESC")
    List<Chat> findUserGroups(@Param("userId") Long userId);

    /**
     * Count groups for a user
     */
    @Query("SELECT COUNT(c) FROM Chat c JOIN ChatMember cm ON c.id = cm.chatId WHERE cm.userId = :userId AND c.type = 'group'")
    long countUserGroups(@Param("userId") Long userId);

    /**
     * Find groups created by a user
     */
    List<Chat> findByCreatedByAndType(Long createdBy, Chat.ChatType type);

    /**
     * Find by type
     */
    List<Chat> findByType(Chat.ChatType type);

}
