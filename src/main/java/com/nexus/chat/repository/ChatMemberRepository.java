package com.nexus.chat.repository;

import com.nexus.chat.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    
    List<ChatMember> findByChatId(Long chatId);
    
    List<ChatMember> findByUserId(Long userId);
    
    Optional<ChatMember> findByChatIdAndUserId(Long chatId, Long userId);
    
    boolean existsByChatIdAndUserId(Long chatId, Long userId);
    
}
