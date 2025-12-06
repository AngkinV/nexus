package com.nexus.chat.controller;

import com.nexus.chat.dto.UserDTO;
import com.nexus.chat.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<Void> addContact(
            @RequestParam Long userId,
            @RequestParam Long contactUserId) {
        try {
            contactService.addContact(userId, contactUserId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> removeContact(
            @RequestParam Long userId,
            @RequestParam Long contactUserId) {
        contactService.removeContact(userId, contactUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserDTO>> getContacts(@PathVariable Long userId) {
        List<UserDTO> contacts = contactService.getContacts(userId);
        return ResponseEntity.ok(contacts);
    }

}
