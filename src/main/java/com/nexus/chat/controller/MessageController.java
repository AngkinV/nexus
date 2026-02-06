package com.nexus.chat.controller;

import com.nexus.chat.dto.MessageDTO;
import com.nexus.chat.model.Message;
import com.nexus.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            Long chatId = Long.valueOf(request.get("chatId").toString());
            Long senderId = Long.valueOf(request.get("senderId").toString());
            String content = (String) request.get("content");
            String messageTypeStr = (String) request.get("messageType");
            String fileUrl = (String) request.get("fileUrl");
            String clientMsgId = (String) request.get("clientMsgId");

            Message.MessageType messageType = messageTypeStr != null
                    ? Message.MessageType.valueOf(messageTypeStr)
                    : Message.MessageType.text;

            MessageDTO message = messageService.sendMessage(chatId, senderId, content, messageType, fileUrl, clientMsgId);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageDTO>> getChatMessages(
            @PathVariable Long chatId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            List<MessageDTO> messages = messageService.getChatMessages(chatId, userId, page, size);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(
            @PathVariable Long messageId,
            @RequestParam Long userId) {
        try {
            messageService.markMessageAsRead(messageId, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/chat/{chatId}/read")
    public ResponseEntity<Void> markChatMessagesAsRead(
            @PathVariable Long chatId,
            @RequestParam Long userId) {
        try {
            messageService.markChatMessagesAsRead(chatId, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
