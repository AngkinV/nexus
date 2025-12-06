package com.nexus.chat.controller;

import com.nexus.chat.dto.UserDTO;
import com.nexus.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        try {
            UserDTO user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {
        try {
            String nickname = updates.get("nickname");
            String avatarUrl = updates.get("avatarUrl");
            UserDTO updated = userService.updateProfile(id, nickname, avatarUrl);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOnlineStatus(
            @PathVariable Long id,
            @RequestParam boolean isOnline) {
        userService.updateOnlineStatus(id, isOnline);
        return ResponseEntity.ok().build();
    }

}
