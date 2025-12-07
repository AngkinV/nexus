package com.nexus.chat.controller;

import com.nexus.chat.dto.AddContactRequest;
import com.nexus.chat.dto.ContactDTO;
import com.nexus.chat.dto.UserDTO;
import com.nexus.chat.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Contact management
 */
@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * Add a contact
     * POST /api/contacts
     */
    @PostMapping
    public ResponseEntity<ContactDTO> addContact(@RequestBody AddContactRequest request) {
        try {
            ContactDTO contact = contactService.addContact(request.getUserId(), request.getContactUserId());
            return ResponseEntity.ok(contact);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove a contact
     * DELETE /api/contacts
     */
    @DeleteMapping
    public ResponseEntity<Void> removeContact(@RequestBody AddContactRequest request) {
        try {
            contactService.removeContact(request.getUserId(), request.getContactUserId());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get contacts list (basic info)
     * GET /api/contacts/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserDTO>> getContacts(@PathVariable Long userId) {
        List<UserDTO> contacts = contactService.getContacts(userId);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Get contacts list with detailed info (respects privacy)
     * GET /api/contacts/user/{userId}/detailed
     */
    @GetMapping("/user/{userId}/detailed")
    public ResponseEntity<List<ContactDTO>> getContactsDetailed(@PathVariable Long userId) {
        List<ContactDTO> contacts = contactService.getContactsDetailed(userId);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Check if user is a contact
     * GET /api/contacts/check?userId={userId}&contactUserId={contactUserId}
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> isContact(
            @RequestParam Long userId,
            @RequestParam Long contactUserId) {
        boolean isContact = contactService.isContact(userId, contactUserId);
        return ResponseEntity.ok(Map.of("isContact", isContact));
    }

    /**
     * Get mutual contacts between two users
     * GET /api/contacts/mutual?userId1={userId1}&userId2={userId2}
     */
    @GetMapping("/mutual")
    public ResponseEntity<List<UserDTO>> getMutualContacts(
            @RequestParam Long userId1,
            @RequestParam Long userId2) {
        List<UserDTO> mutualContacts = contactService.getMutualContacts(userId1, userId2);
        return ResponseEntity.ok(mutualContacts);
    }

}
