package com.nexus.chat.service;

import com.nexus.chat.dto.UserDTO;
import com.nexus.chat.model.Contact;
import com.nexus.chat.model.User;
import com.nexus.chat.repository.ContactRepository;
import com.nexus.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addContact(Long userId, Long contactUserId) {
        // Check if contact already exists
        if (contactRepository.existsByUserIdAndContactUserId(userId, contactUserId)) {
            throw new RuntimeException("Contact already exists");
        }

        // Check if contact user exists
        if (!userRepository.existsById(contactUserId)) {
            throw new RuntimeException("Contact user not found");
        }

        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setContactUserId(contactUserId);
        contactRepository.save(contact);
    }

    @Transactional
    public void removeContact(Long userId, Long contactUserId) {
        contactRepository.deleteByUserIdAndContactUserId(userId, contactUserId);
    }

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

}
