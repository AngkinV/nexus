package com.nexus.chat.websocket;

import com.nexus.chat.dto.*;
import com.nexus.chat.model.ChatMember;
import com.nexus.chat.model.Message;
import com.nexus.chat.repository.ChatMemberRepository;
import com.nexus.chat.service.ContactService;
import com.nexus.chat.service.GroupService;
import com.nexus.chat.service.MessageService;
import com.nexus.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * WebSocket Controller for real-time messaging and events
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final ContactService contactService;
    private final GroupService groupService;
    private final ChatMemberRepository chatMemberRepository;

    /**
     * Handle sending chat messages (direct and group)
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload) {
        try {
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            String content = (String) payload.get("content");
            String messageTypeStr = (String) payload.get("messageType");
            String fileUrl = (String) payload.get("fileUrl");

            Message.MessageType messageType = messageTypeStr != null
                    ? Message.MessageType.valueOf(messageTypeStr)
                    : Message.MessageType.text;

            MessageDTO message = messageService.sendMessage(chatId, senderId, content, messageType, fileUrl);

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.CHAT_MESSAGE,
                    message);

            // Broadcast to chat room topic
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);

            // Also send directly to each chat member's personal topic
            // This ensures delivery even if they haven't subscribed to the chat room
            List<ChatMember> members = chatMemberRepository.findByChatId(chatId);
            for (ChatMember member : members) {
                // Send to user-specific topic (more reliable than user destinations)
                messagingTemplate.convertAndSend(
                        "/topic/user." + member.getUserId() + ".messages",
                        wsMessage
                );
            }
        } catch (Exception e) {
            log.error("发送消息失败: chatId={}, senderId={}", payload.get("chatId"), payload.get("senderId"), e);
        }
    }

    /**
     * Handle user status updates (online/offline)
     */
    @MessageMapping("/user.status")
    public void updateUserStatus(@Payload Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Boolean isOnline = (Boolean) payload.get("isOnline");

            userService.updateOnlineStatus(userId, isOnline);

            WebSocketMessage wsMessage = new WebSocketMessage(
                    isOnline ? WebSocketMessage.MessageType.USER_ONLINE : WebSocketMessage.MessageType.USER_OFFLINE,
                    Map.of("userId", userId));

            // Broadcast to all users
            messagingTemplate.convertAndSend("/topic/users", wsMessage);

            // Notify user's contacts about status change
            contactService.notifyContactsOfStatusChange(userId, isOnline);
        } catch (Exception e) {
            log.error("更新用户状态失败: userId={}, isOnline={}", payload.get("userId"), payload.get("isOnline"), e);
        }
    }

    /**
     * Handle typing indicator
     */
    @MessageMapping("/chat.typing")
    public void userTyping(@Payload Map<String, Object> payload) {
        try {
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            Boolean isTyping = (Boolean) payload.get("isTyping");

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.TYPING,
                    Map.of("userId", userId, "isTyping", isTyping));

            // Broadcast to chat room
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
        } catch (Exception e) {
            log.error("处理输入状态失败: chatId={}, userId={}", payload.get("chatId"), payload.get("userId"), e);
        }
    }

    /**
     * Handle message read status
     */
    @MessageMapping("/message.read")
    public void messageRead(@Payload Map<String, Object> payload) {
        try {
            Long chatId = Long.valueOf(payload.get("chatId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long messageId = payload.get("messageId") != null
                    ? Long.valueOf(payload.get("messageId").toString())
                    : null;

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.MESSAGE_READ,
                    Map.of("chatId", chatId, "userId", userId, "messageId", messageId != null ? messageId : "all"));

            // Broadcast to chat room
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
        } catch (Exception e) {
            log.error("处理消息已读状态失败: chatId={}, userId={}", payload.get("chatId"), payload.get("userId"), e);
        }
    }

    /**
     * Handle group creation
     */
    @MessageMapping("/group.create")
    public void createGroup(@Payload Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String name = (String) payload.get("name");
            String description = (String) payload.get("description");
            Boolean isPrivate = (Boolean) payload.getOrDefault("isPrivate", false);
            @SuppressWarnings("unchecked")
            List<Long> memberIds = (List<Long>) payload.get("memberIds");

            CreateGroupRequest request = new CreateGroupRequest();
            request.setName(name);
            request.setDescription(description);
            request.setIsPrivate(isPrivate);
            request.setMemberIds(memberIds);

            GroupDTO group = groupService.createGroup(userId, request);

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.GROUP_CREATED,
                    group);

            // Notify creator
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/groups", wsMessage);

            // Notify all members
            if (memberIds != null) {
                for (Long memberId : memberIds) {
                    if (!memberId.equals(userId)) {
                        messagingTemplate.convertAndSendToUser(String.valueOf(memberId), "/queue/groups", wsMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("创建群组失败: userId={}, name={}", payload.get("userId"), payload.get("name"), e);
            sendError(payload.get("userId"), "Failed to create group: " + e.getMessage());
        }
    }

    /**
     * Handle joining a group
     */
    @MessageMapping("/group.join")
    public void joinGroup(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long adminUserId = Long.valueOf(payload.get("adminUserId").toString());

            groupService.addMembers(groupId, adminUserId, List.of(userId));

            UserDTO user = userService.getUserById(userId);
            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.GROUP_MEMBER_JOINED,
                    Map.of("groupId", groupId, "user", user));

            // Broadcast to group
            messagingTemplate.convertAndSend("/topic/group/" + groupId, wsMessage);
        } catch (Exception e) {
            log.error("加入群组失败: groupId={}, userId={}", payload.get("groupId"), payload.get("userId"), e);
        }
    }

    /**
     * Handle leaving a group
     */
    @MessageMapping("/group.leave")
    public void leaveGroup(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());

            groupService.leaveGroup(groupId, userId);

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.GROUP_MEMBER_LEFT,
                    Map.of("groupId", groupId, "userId", userId));

            // Broadcast to group
            messagingTemplate.convertAndSend("/topic/group/" + groupId, wsMessage);
        } catch (Exception e) {
            log.error("离开群组失败: groupId={}, userId={}", payload.get("groupId"), payload.get("userId"), e);
        }
    }

    /**
     * Handle group message
     */
    @MessageMapping("/group.message")
    public void sendGroupMessage(@Payload Map<String, Object> payload) {
        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            String content = (String) payload.get("content");
            String messageTypeStr = (String) payload.get("messageType");

            Message.MessageType messageType = messageTypeStr != null
                    ? Message.MessageType.valueOf(messageTypeStr)
                    : Message.MessageType.text;

            MessageDTO message = messageService.sendMessage(groupId, senderId, content, messageType, null);

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.GROUP_MESSAGE,
                    message);

            // Broadcast to group
            messagingTemplate.convertAndSend("/topic/group/" + groupId, wsMessage);
        } catch (Exception e) {
            log.error("发送群组消息失败: groupId={}, senderId={}", payload.get("groupId"), payload.get("senderId"), e);
        }
    }

    /**
     * Handle adding contact
     */
    @MessageMapping("/contact.add")
    public void addContact(@Payload Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long contactUserId = Long.valueOf(payload.get("contactUserId").toString());
            String message = payload.get("message") != null ? payload.get("message").toString() : null;

            Object result = contactService.addContact(userId, contactUserId, message);

            // 根据返回类型决定消息类型
            if (result instanceof ContactDTO) {
                WebSocketMessage wsMessage = new WebSocketMessage(
                        WebSocketMessage.MessageType.CONTACT_ADDED,
                        result);
                messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/contacts", wsMessage);
            }
            // ContactRequestDTO 的情况已在 ContactService 中处理了 WebSocket 通知
        } catch (Exception e) {
            log.error("添加联系人失败: userId={}, contactUserId={}", payload.get("userId"), payload.get("contactUserId"), e);
            sendError(payload.get("userId"), "Failed to add contact: " + e.getMessage());
        }
    }

    /**
     * Handle removing contact
     */
    @MessageMapping("/contact.remove")
    public void removeContact(@Payload Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long contactUserId = Long.valueOf(payload.get("contactUserId").toString());

            contactService.removeContact(userId, contactUserId);

            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.CONTACT_REMOVED,
                    Map.of("contactId", contactUserId));

            // Notify user
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/contacts", wsMessage);
        } catch (Exception e) {
            log.error("移除联系人失败: userId={}, contactUserId={}", payload.get("userId"), payload.get("contactUserId"), e);
        }
    }

    /**
     * Send error message to user
     */
    private void sendError(Object userId, String errorMessage) {
        if (userId != null) {
            WebSocketMessage wsMessage = new WebSocketMessage(
                    WebSocketMessage.MessageType.ERROR,
                    Map.of("message", errorMessage));
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/errors", wsMessage);
        }
    }

}
