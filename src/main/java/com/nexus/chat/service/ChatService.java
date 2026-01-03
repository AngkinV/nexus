package com.nexus.chat.service;

import com.nexus.chat.dto.ChatDTO;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.MessageDTO;
import com.nexus.chat.dto.UserDTO;
import com.nexus.chat.dto.WebSocketMessage;
import com.nexus.chat.exception.BusinessException;
import com.nexus.chat.model.Chat;
import com.nexus.chat.model.ChatMember;
import com.nexus.chat.model.Message;
import com.nexus.chat.model.User;
import com.nexus.chat.repository.ChatMemberRepository;
import com.nexus.chat.repository.ChatRepository;
import com.nexus.chat.repository.MessageRepository;
import com.nexus.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatDTO createDirectChat(Long userId, Long contactId) {
        // Check if direct chat already exists between these two users
        Optional<Chat> existingChat = findExistingDirectChat(userId, contactId);
        if (existingChat.isPresent()) {
            return mapToDTO(existingChat.get(), userId);
        }

        // Create new direct chat
        Chat chat = new Chat();
        chat.setType(Chat.ChatType.direct);
        chat.setCreatedBy(userId);
        Chat savedChat = chatRepository.save(chat);

        // Add both users as members
        ChatMember member1 = new ChatMember();
        member1.setChatId(savedChat.getId());
        member1.setUserId(userId);
        member1.setIsAdmin(false);
        chatMemberRepository.save(member1);

        ChatMember member2 = new ChatMember();
        member2.setChatId(savedChat.getId());
        member2.setUserId(contactId);
        member2.setIsAdmin(false);
        chatMemberRepository.save(member2);

        ChatDTO chatDTO = mapToDTO(savedChat, userId);

        // Notify the contact about the new chat via WebSocket
        ChatDTO contactChatDTO = mapToDTO(savedChat, contactId);
        WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.MessageType.CHAT_CREATED,
                contactChatDTO);
        messagingTemplate.convertAndSendToUser(String.valueOf(contactId), "/queue/chats", wsMessage);

        return chatDTO;
    }

    @Transactional
    public ChatDTO createGroupChat(Long userId, CreateGroupRequest request) {
        // Validation
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException("Group name is required");
        }
        if (request.getName().length() > 100) {
            throw new BusinessException("Group name must be less than 100 characters");
        }
        if (request.getDescription() != null && request.getDescription().length() > 200) {
            throw new BusinessException("Group description must be less than 200 characters");
        }
        if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            throw new BusinessException("At least one member is required");
        }
        if (request.getMemberIds().size() > 200) {
            throw new BusinessException("Group cannot have more than 200 members");
        }

        // Verify all member IDs exist
        for (Long memberId : request.getMemberIds()) {
            if (!memberId.equals(userId) && !userRepository.existsById(memberId)) {
                throw new BusinessException("User with ID " + memberId + " not found");
            }
        }

        // Create group chat
        Chat chat = new Chat();
        chat.setType(Chat.ChatType.group);
        chat.setName(request.getName().trim());
        chat.setDescription(request.getDescription());
        chat.setAvatarUrl(request.getAvatar());
        chat.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);
        chat.setCreatedBy(userId);

        // Calculate member count (creator + other members)
        int memberCount = 1 + (request.getMemberIds() != null ?
            (int) request.getMemberIds().stream().filter(id -> !id.equals(userId)).count() : 0);
        chat.setMemberCount(memberCount);

        Chat savedChat = chatRepository.save(chat);

        // Add creator as admin member
        ChatMember creatorMember = new ChatMember();
        creatorMember.setChatId(savedChat.getId());
        creatorMember.setUserId(userId);
        creatorMember.setIsAdmin(true);
        chatMemberRepository.save(creatorMember);

        // Add other members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    ChatMember member = new ChatMember();
                    member.setChatId(savedChat.getId());
                    member.setUserId(memberId);
                    member.setIsAdmin(false);
                    chatMemberRepository.save(member);
                }
            }
        }

        ChatDTO chatDTO = mapToDTO(savedChat, userId);

        // Notify all members about the new group via WebSocket
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    ChatDTO memberChatDTO = mapToDTO(savedChat, memberId);
                    WebSocketMessage wsMessage = new WebSocketMessage(
                            WebSocketMessage.MessageType.CHAT_CREATED,
                            memberChatDTO);
                    messagingTemplate.convertAndSendToUser(String.valueOf(memberId), "/queue/chats", wsMessage);
                }
            }
        }

        return chatDTO;
    }

    public List<ChatDTO> getUserChats(Long userId) {
        List<Chat> chats = chatRepository.findByUserIdOrderByLastMessageAtDesc(userId);
        return chats.stream()
                .map(chat -> mapToDTO(chat, userId))
                .collect(Collectors.toList());
    }

    public ChatDTO getChatById(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new BusinessException("error.chat.not.found"));

        // Verify user is a member
        if (!chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new BusinessException("error.chat.not.member");
        }

        return mapToDTO(chat, userId);
    }

    private Optional<Chat> findExistingDirectChat(Long userId1, Long userId2) {
        List<ChatMember> userChats = chatMemberRepository.findByUserId(userId1);
        for (ChatMember member : userChats) {
            Chat chat = chatRepository.findById(member.getChatId()).orElse(null);
            if (chat != null && chat.getType() == Chat.ChatType.direct) {
                // Check if other user is also in this chat
                if (chatMemberRepository.existsByChatIdAndUserId(chat.getId(), userId2)) {
                    return Optional.of(chat);
                }
            }
        }
        return Optional.empty();
    }

    private ChatDTO mapToDTO(Chat chat, Long currentUserId) {
        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setType(chat.getType());
        dto.setName(chat.getName());
        dto.setDescription(chat.getDescription());
        dto.setAvatar(chat.getAvatarUrl());
        dto.setIsPrivate(chat.getIsPrivate());
        dto.setCreatedBy(chat.getCreatedBy());
        dto.setMemberCount(chat.getMemberCount());
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setLastMessageAt(chat.getLastMessageAt());

        // Get chat members
        List<ChatMember> members = chatMemberRepository.findByChatId(chat.getId());
        List<UserDTO> memberDTOs = members.stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    if (user != null) {
                        return new UserDTO(
                                user.getId(),
                                user.getUsername(),
                                user.getNickname(),
                                user.getAvatarUrl(),
                                user.getIsOnline(),
                                user.getLastSeen());
                    }
                    return null;
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
        dto.setMembers(memberDTOs);

        // For direct chat, set name as the other user's nickname
        if (chat.getType() == Chat.ChatType.direct && dto.getName() == null) {
            Optional<UserDTO> otherUser = memberDTOs.stream()
                    .filter(u -> !u.getId().equals(currentUserId))
                    .findFirst();
            otherUser.ifPresent(user -> dto.setName(user.getNickname()));
        }

        // Get last message
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId());
        if (!messages.isEmpty()) {
            Message lastMsg = messages.get(0);
            User sender = userRepository.findById(lastMsg.getSenderId()).orElse(null);
            if (sender != null) {
                MessageDTO msgDTO = new MessageDTO(
                        lastMsg.getId(),
                        lastMsg.getChatId(),
                        lastMsg.getSenderId(),
                        sender.getNickname(),
                        sender.getAvatarUrl(),
                        lastMsg.getContent(),
                        lastMsg.getMessageType(),
                        lastMsg.getFileUrl(),
                        lastMsg.getCreatedAt(),
                        null);
                dto.setLastMessage(msgDTO);
            }
        }

        // Get unread count for current user
        ChatMember currentMember = chatMemberRepository
                .findByChatIdAndUserId(chat.getId(), currentUserId)
                .orElse(null);
        if (currentMember != null) {
            dto.setUnreadCount(currentMember.getUnreadCount());
        }

        return dto;
    }

}
