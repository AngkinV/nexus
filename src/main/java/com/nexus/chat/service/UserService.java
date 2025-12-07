package com.nexus.chat.service;

import com.nexus.chat.dto.*;
import com.nexus.chat.model.User;
import com.nexus.chat.model.UserPrivacySettings;
import com.nexus.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final ContactRepository contactRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    // Avatar upload directory (can be configured)
    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDTO(user);
    }

    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateProfile(Long userId, String nickname, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        User updated = userRepository.save(user);
        return mapToDTO(updated);
    }

    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsOnline(isOnline);
        userRepository.save(user);
    }

    /**
     * Get full user profile with privacy settings
     */
    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPrivacySettings privacy = privacySettingsRepository.findByUserId(userId)
                .orElse(createDefaultPrivacySettings(userId));

        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setBio(user.getBio());
        profile.setIsOnline(user.getIsOnline());
        profile.setLastSeen(user.getLastSeen());
        profile.setCreatedAt(user.getCreatedAt());

        // Privacy settings
        profile.setShowOnlineStatus(privacy.getShowOnlineStatus());
        profile.setShowLastSeen(privacy.getShowLastSeen());
        profile.setShowEmail(privacy.getShowEmail());
        profile.setShowPhone(privacy.getShowPhone());

        // Statistics
        profile.setContactCount((long) contactRepository.findByUserId(userId).size());
        profile.setGroupCount(chatRepository.countUserGroups(userId));
        profile.setMessageCount(messageRepository.countBySenderId(userId));

        return profile;
    }

    /**
     * Get user profile for viewing by another user (respects privacy settings)
     */
    public UserProfileDTO getUserProfileForViewer(Long userId, Long viewerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPrivacySettings privacy = privacySettingsRepository.findByUserId(userId)
                .orElse(createDefaultPrivacySettings(userId));

        UserProfileDTO profile = new UserProfileDTO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setBio(user.getBio());
        profile.setCreatedAt(user.getCreatedAt());

        // Apply privacy settings
        if (privacy.getShowOnlineStatus()) {
            profile.setIsOnline(user.getIsOnline());
        }
        if (privacy.getShowLastSeen()) {
            profile.setLastSeen(user.getLastSeen());
        }
        if (privacy.getShowEmail()) {
            profile.setEmail(user.getEmail());
        }
        if (privacy.getShowPhone()) {
            profile.setPhone(user.getPhone());
        }

        return profile;
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserProfileDTO updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            if (request.getNickname().length() < 2 || request.getNickname().length() > 30) {
                throw new RuntimeException("Nickname must be between 2 and 30 characters");
            }
            user.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            if (request.getBio().length() > 150) {
                throw new RuntimeException("Bio must be at most 150 characters");
            }
            user.setBio(request.getBio());
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // Check if email is already used by another user
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(userId)) {
                            throw new RuntimeException("Email is already in use");
                        }
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);
        return getUserProfile(userId);
    }

    /**
     * Upload user avatar
     */
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString() + extension;

        // Create upload directory if not exists
        Path uploadPath = Paths.get(AVATAR_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Update user avatar URL
        String avatarUrl = "/uploads/avatars/" + filename;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    /**
     * Upload avatar as Base64
     */
    @Transactional
    public String uploadAvatarBase64(Long userId, String base64Image) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // For simplicity, store Base64 directly (suitable for smaller images)
        // In production, consider decoding and saving as file
        user.setAvatarUrl(base64Image);
        userRepository.save(user);

        return base64Image;
    }

    /**
     * Delete user avatar
     */
    @Transactional
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old file if it's a path
        String oldAvatar = user.getAvatarUrl();
        if (oldAvatar != null && oldAvatar.startsWith("/uploads/")) {
            try {
                Path oldPath = Paths.get(oldAvatar.substring(1));
                Files.deleteIfExists(oldPath);
            } catch (IOException e) {
                // Ignore deletion errors
            }
        }

        user.setAvatarUrl(null);
        userRepository.save(user);
    }

    /**
     * Update privacy settings
     */
    @Transactional
    public PrivacySettingsDTO updatePrivacySettings(Long userId, PrivacySettingsDTO request) {
        UserPrivacySettings settings = privacySettingsRepository.findByUserId(userId)
                .orElse(createDefaultPrivacySettings(userId));

        if (request.getShowOnlineStatus() != null) {
            settings.setShowOnlineStatus(request.getShowOnlineStatus());
        }
        if (request.getShowLastSeen() != null) {
            settings.setShowLastSeen(request.getShowLastSeen());
        }
        if (request.getShowEmail() != null) {
            settings.setShowEmail(request.getShowEmail());
        }
        if (request.getShowPhone() != null) {
            settings.setShowPhone(request.getShowPhone());
        }

        UserPrivacySettings saved = privacySettingsRepository.save(settings);

        PrivacySettingsDTO response = new PrivacySettingsDTO();
        response.setShowOnlineStatus(saved.getShowOnlineStatus());
        response.setShowLastSeen(saved.getShowLastSeen());
        response.setShowEmail(saved.getShowEmail());
        response.setShowPhone(saved.getShowPhone());

        return response;
    }

    /**
     * Get user statistics
     */
    public UserStatsDTO getUserStats(Long userId) {
        long contactCount = contactRepository.findByUserId(userId).size();
        long groupCount = chatRepository.countUserGroups(userId);
        long messageCount = messageRepository.countBySenderId(userId);

        return new UserStatsDTO(contactCount, groupCount, messageCount);
    }

    /**
     * Search users by query
     */
    public List<UserDTO> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.searchUsers(query.trim());
        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get recommended users
     */
    public List<UserDTO> getRecommendedUsers(Long userId, int limit) {
        List<User> users = userRepository.findRecommendedUsers(userId, limit);
        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create default privacy settings for user
     */
    private UserPrivacySettings createDefaultPrivacySettings(Long userId) {
        UserPrivacySettings settings = new UserPrivacySettings();
        settings.setUserId(userId);
        settings.setShowOnlineStatus(true);
        settings.setShowLastSeen(true);
        settings.setShowEmail(false);
        settings.setShowPhone(false);
        return privacySettingsRepository.save(settings);
    }

    private UserDTO mapToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getIsOnline(),
                user.getLastSeen());
    }

}
