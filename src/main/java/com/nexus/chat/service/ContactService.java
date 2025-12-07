package com.nexus.chat.service;

import com.nexus.chat.dto.ContactDTO;
import com.nexus.chat.dto.UserDTO;
import com.nexus.chat.dto.WebSocketMessage;
import com.nexus.chat.model.Contact;
import com.nexus.chat.model.User;
import com.nexus.chat.model.UserPrivacySettings;
import com.nexus.chat.repository.ContactRepository;
import com.nexus.chat.repository.UserPrivacySettingsRepository;
import com.nexus.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Add a contact
     */
    @Transactional
    public ContactDTO addContact(Long userId, Long contactUserId) {
        // Can't add yourself
        if (userId.equals(contactUserId)) {
            throw new RuntimeException("Cannot add yourself as a contact");
        }

        // Check if contact already exists
        if (contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)) {
            throw new RuntimeException("Contact already exists");
        }

        // Check if contact user exists
        User contactUser = userRepository.findById(contactUserId)
                .orElseThrow(() -> new RuntimeException("Contact user not found"));

        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactUserId(contactUserId);
        Contact savedContact = contactRepository.save(contact);

        // Build contact DTO
        ContactDTO contactDTO = mapToContactDTO(savedContact, contactUser);

        // Notify user via WebSocket
        WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.MessageType.CONTACT_ADDED,
                contactDTO);
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/contacts", wsMessage);

        return contactDTO;
    }

    /**
     * Remove a contact
     */
    @Transactional
    public void removeContact(Long userId, Long contactUserId) {
        Contact contact = contactRepository.findByUserIdAndContactUserId(userId, contactUserId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contactRepository.delete(contact);

        // Notify user via WebSocket
        WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.MessageType.CONTACT_REMOVED,
                Map.of("contactId", contactUserId));
        messagingTemplate.convertAndSend("/user/" + userId + "/queue/contacts", wsMessage);
    }

    /**
     * Get contacts list with user details
     */
    public List<UserDTO> getContacts(Long userId) {
        List<Contact> contacts = contactRepository.findByUserId(userId);
        return contacts.stream()
                .map(contact -> {
                    User user = userRepository.findById(contact.getContactUserId()).orElse(null);
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
    }

    /**
     * Get contacts list with detailed information (respecting privacy settings)
     */
    public List<ContactDTO> getContactsDetailed(Long userId) {
        List<Contact> contacts = contactRepository.findByUserId(userId);
        return contacts.stream()
                .map(contact -> {
                    User user = userRepository.findById(contact.getContactUserId()).orElse(null);
                    if (user != null) {
                        return mapToContactDTOWithPrivacy(contact, user);
                    }
                    return null;
                })
                .filter(c -> c != null)
                .collect(Collectors.toList());
    }

    /**
     * Check if a user is a contact
     */
    public boolean isContact(Long userId, Long contactUserId) {
        return contactRepository.existsByUserIdAndContactUserId(userId, contactUserId);
    }

    /**
     * Get mutual contacts between two users
     */
    public List<UserDTO> getMutualContacts(Long userId1, Long userId2) {
        List<Contact> user1Contacts = contactRepository.findByUserId(userId1);
        List<Contact> user2Contacts = contactRepository.findByUserId(userId2);

        List<Long> user1ContactIds = user1Contacts.stream()
                .map(Contact::getContactUserId)
                .collect(Collectors.toList());

        return user2Contacts.stream()
                .filter(c -> user1ContactIds.contains(c.getContactUserId()))
                .map(contact -> {
                    User user = userRepository.findById(contact.getContactUserId()).orElse(null);
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
    }

    /**
     * Notify contacts about user status change
     */
    public void notifyContactsOfStatusChange(Long userId, boolean isOnline) {
        // Find users who have this user as a contact
        List<Contact> allContacts = contactRepository.findAll();
        List<Long> usersWhoHaveThisUserAsContact = allContacts.stream()
                .filter(c -> c.getContactUserId().equals(userId))
                .map(Contact::getUserId)
                .collect(Collectors.toList());

        WebSocketMessage wsMessage = new WebSocketMessage(
                WebSocketMessage.MessageType.CONTACT_STATUS_CHANGED,
                Map.of("userId", userId, "isOnline", isOnline));

        // Notify each user who has this user as a contact
        for (Long targetUserId : usersWhoHaveThisUserAsContact) {
            messagingTemplate.convertAndSend("/user/" + targetUserId + "/queue/contacts", wsMessage);
        }
    }

    /**
     * Map Contact and User to ContactDTO
     */
    private ContactDTO mapToContactDTO(Contact contact, User user) {
        return new ContactDTO(
                contact.getId(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getIsOnline(),
                user.getLastSeen(),
                contact.getCreatedAt());
    }

    /**
     * Map Contact and User to ContactDTO with privacy settings applied
     */
    private ContactDTO mapToContactDTOWithPrivacy(Contact contact, User user) {
        UserPrivacySettings privacy = privacySettingsRepository.findByUserId(user.getId())
                .orElse(null);

        ContactDTO dto = new ContactDTO();
        dto.setId(contact.getId());
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setAddedAt(contact.getCreatedAt());

        // Apply privacy settings
        if (privacy == null || privacy.getShowOnlineStatus()) {
            dto.setIsOnline(user.getIsOnline());
        }
        if (privacy == null || privacy.getShowLastSeen()) {
            dto.setLastSeen(user.getLastSeen());
        }
        if (privacy != null && privacy.getShowEmail()) {
            dto.setEmail(user.getEmail());
        }
        if (privacy != null && privacy.getShowPhone()) {
            dto.setPhone(user.getPhone());
        }

        return dto;
    }

}
