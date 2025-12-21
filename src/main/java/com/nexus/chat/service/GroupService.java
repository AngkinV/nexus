package com.nexus.chat.service;

import com.nexus.chat.dto.*;
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
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new group
     */
    @Transactional
    public GroupDTO createGroup(Long creatorId, CreateGroupRequest request) {
        // Create group chat
        Chat chat = new Chat();
        chat.setType(Chat.ChatType.group);
        chat.setName(request.getName());
        chat.setDescription(request.getDescription());
        chat.setAvatarUrl(request.getAvatar());
        chat.setIsPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false);
        chat.setCreatedBy(creatorId);
        chat.setMemberCount(1); // Creator is the first member
        Chat savedChat = chatRepository.save(chat);

        // Add creator as owner
        ChatMember creatorMember = new ChatMember();
        creatorMember.setChatId(savedChat.getId());
        creatorMember.setUserId(creatorId);
        creatorMember.setRole(ChatMember.MemberRole.owner);
        creatorMember.setIsAdmin(true);
        chatMemberRepository.save(creatorMember);

        // Add other members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {
                    ChatMember member = new ChatMember();
                    member.setChatId(savedChat.getId());
                    member.setUserId(memberId);
                    member.setRole(ChatMember.MemberRole.member);
                    member.setIsAdmin(false);
                    chatMemberRepository.save(member);
                }
            }
            // Update member count
            savedChat.setMemberCount(request.getMemberIds().size() + 1);
            chatRepository.save(savedChat);
        }

        GroupDTO groupDTO = mapToGroupDTO(savedChat);

        // Broadcast group creation
        broadcastGroupEvent("group:created", groupDTO, savedChat.getId());

        return groupDTO;
    }

    /**
     * Get group by ID
     */
    public GroupDTO getGroupById(Long groupId) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("error.group.not.found"));

        if (chat.getType() != Chat.ChatType.group) {
            throw new BusinessException("error.group.not.group.chat");
        }

        return mapToGroupDTO(chat);
    }

    /**
     * Update group information
     */
    @Transactional
    public GroupDTO updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("error.group.not.found"));

        // Check if user is admin
        ChatMember member = chatMemberRepository.findByChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("error.chat.not.member"));

        if (!member.getIsAdmin()) {
            throw new BusinessException("error.group.admin.required");
        }

        // Update fields
        if (request.getName() != null) {
            chat.setName(request.getName());
        }
        if (request.getDescription() != null) {
            chat.setDescription(request.getDescription());
        }
        if (request.getAvatar() != null) {
            chat.setAvatarUrl(request.getAvatar());
        }
        if (request.getIsPrivate() != null) {
            chat.setIsPrivate(request.getIsPrivate());
        }

        Chat updatedChat = chatRepository.save(chat);
        GroupDTO groupDTO = mapToGroupDTO(updatedChat);

        // Broadcast group update
        broadcastGroupEvent("group:updated", groupDTO, groupId);

        return groupDTO;
    }

    /**
     * Delete/disband group
     */
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("error.group.not.found"));

        // Only owner can delete group
        if (!chat.getCreatedBy().equals(userId)) {
            throw new BusinessException("error.group.owner.required");
        }

        // Broadcast deletion before deleting
        broadcastGroupEvent("group:deleted", Map.of("groupId", groupId), groupId);

        // Delete all members first
        chatMemberRepository.deleteByChatId(groupId);

        // Delete group
        chatRepository.delete(chat);
    }

    /**
     * Add members to group
     */
    @Transactional
    public void addMembers(Long groupId, Long userId, List<Long> userIds) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("error.group.not.found"));

        // Check if user is admin
        ChatMember adminMember = chatMemberRepository.findByChatIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("error.chat.not.member"));

        if (!adminMember.getIsAdmin()) {
            throw new BusinessException("error.group.admin.add.member");
        }

        for (Long newUserId : userIds) {
            if (!chatMemberRepository.existsByChatIdAndUserId(groupId, newUserId)) {
                // Verify user exists
                User user = userRepository.findById(newUserId)
                        .orElseThrow(() -> new BusinessException("error.user.not.found"));

                ChatMember member = new ChatMember();
                member.setChatId(groupId);
                member.setUserId(newUserId);
                member.setRole(ChatMember.MemberRole.member);
                member.setIsAdmin(false);
                chatMemberRepository.save(member);

                // Broadcast member joined
                UserDTO userDTO = mapToUserDTO(user);
                broadcastGroupEvent("group:member-joined",
                        Map.of("groupId", groupId, "user", userDTO), groupId);
            }
        }

        // Update member count
        chat.setMemberCount((int) chatMemberRepository.countByChatId(groupId));
        chatRepository.save(chat);
    }

    /**
     * Remove member from group
     */
    @Transactional
    public void removeMember(Long groupId, Long adminUserId, Long memberUserId) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("error.group.not.found"));

        // Check if requesting user is admin
        ChatMember adminMember = chatMemberRepository.findByChatIdAndUserId(groupId, adminUserId)
                .orElseThrow(() -> new BusinessException("error.chat.not.member"));

        if (!adminMember.getIsAdmin()) {
            throw new BusinessException("error.group.admin.remove.member");
        }

        // Can't remove owner
        if (chat.getCreatedBy().equals(memberUserId)) {
            throw new BusinessException("error.group.cannot.remove.owner");
        }

        chatMemberRepository.deleteByChatIdAndUserId(groupId, memberUserId);

        // Update member count
        chat.setMemberCount((int) chatMemberRepository.countByChatId(groupId));
        chatRepository.save(chat);

        // Broadcast member removed
        broadcastGroupEvent("group:member-left",
                Map.of("groupId", groupId, "userId", memberUserId), groupId);
    }

    /**
     * Leave group
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Chat chat = chatRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("error.group.not.found"));

        // Owner cannot leave (must delete group)
        if (chat.getCreatedBy().equals(userId)) {
            throw new BusinessException("error.group.owner.cannot.leave");
        }

        chatMemberRepository.deleteByChatIdAndUserId(groupId, userId);

        // Update member count
        chat.setMemberCount((int) chatMemberRepository.countByChatId(groupId));
        chatRepository.save(chat);

        // Broadcast member left
        broadcastGroupEvent("group:member-left",
                Map.of("groupId", groupId, "userId", userId), groupId);
    }

    /**
     * Get group members
     */
    public List<UserDTO> getGroupMembers(Long groupId) {
        List<ChatMember> members = chatMemberRepository.findByChatId(groupId);
        return members.stream()
                .map(member -> {
                    User user = userRepository.findById(member.getUserId()).orElse(null);
                    if (user != null) {
                        return mapToUserDTO(user);
                    }
                    return null;
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
    }

    /**
     * Get groups for a user
     */
    public List<GroupDTO> getUserGroups(Long userId) {
        List<Chat> groups = chatRepository.findUserGroups(userId);
        return groups.stream()
                .map(this::mapToGroupDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map Chat to GroupDTO
     */
    private GroupDTO mapToGroupDTO(Chat chat) {
        GroupDTO dto = new GroupDTO();
        dto.setId(chat.getId());
        dto.setName(chat.getName());
        dto.setDescription(chat.getDescription());
        dto.setAvatar(chat.getAvatarUrl());
        dto.setType(chat.getType());
        dto.setIsPrivate(chat.getIsPrivate());
        dto.setCreatorId(chat.getCreatedBy());
        dto.setMemberCount(chat.getMemberCount());
        dto.setCreatedAt(chat.getCreatedAt());

        // Get members
        List<UserDTO> members = getGroupMembers(chat.getId());
        dto.setMembers(members);

        // Get last message
        List<Message> messages = messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId());
        if (!messages.isEmpty()) {
            Message lastMsg = messages.get(0);
            dto.setLastMessage(lastMsg.getContent());
            dto.setLastMessageTime(lastMsg.getCreatedAt());
        }

        return dto;
    }

    /**
     * Map User to UserDTO
     */
    private UserDTO mapToUserDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getIsOnline(),
                user.getLastSeen());
    }

    /**
     * Broadcast group event to all members
     */
    private void broadcastGroupEvent(String eventType, Object payload, Long groupId) {
        WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.MessageType.valueOf(eventType.replace(":", "_").toUpperCase()),
                payload);
        messagingTemplate.convertAndSend("/topic/group/" + groupId, wsMessage);
    }

}
