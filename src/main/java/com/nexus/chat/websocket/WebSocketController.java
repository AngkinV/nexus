package com.nexus.chat.websocket;

import com.nexus.chat.dto.MessageDTO;
import com.nexus.chat.dto.WebSocketMessage;
import com.nexus.chat.model.Message;
import com.nexus.chat.service.MessageService;
import com.nexus.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final UserService userService;

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

            // Broadcast to chat room
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            e.printStackTrace();
        }
    }

}
