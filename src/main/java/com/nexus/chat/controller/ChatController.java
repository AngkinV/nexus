package com.nexus.chat.controller;

import com.nexus.chat.dto.ChatDTO;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/direct")
    public ResponseEntity<ChatDTO> createDirectChat(
            @RequestParam Long userId,
            @RequestParam Long contactId) {
        try {
            ChatDTO chat = chatService.createDirectChat(userId, contactId);
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/group")
    public ResponseEntity<ChatDTO> createGroupChat(
            @RequestParam Long userId,
            @RequestBody CreateGroupRequest request) {
        try {
            ChatDTO chat = chatService.createGroupChat(userId, request);
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatDTO>> getUserChats(@PathVariable Long userId) {
        List<ChatDTO> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDTO> getChatById(
            @PathVariable Long chatId,
            @RequestParam Long userId) {
        try {
            ChatDTO chat = chatService.getChatById(chatId, userId);
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
