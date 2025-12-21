package com.nexus.chat.controller;

import com.nexus.chat.dto.*;
import com.nexus.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for User management
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user by username
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        try {
            UserDTO user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all users
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Search users by query (username, nickname, or email)
     * GET /api/users/search?query={query}
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        List<UserDTO> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    /**
     * Get recommended users (for adding contacts)
     * GET /api/users/recommended?userId={userId}&limit={limit}
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<UserDTO>> getRecommendedUsers(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<UserDTO> users = userService.getRecommendedUsers(userId, limit);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user profile (with privacy settings and stats)
     * GET /api/users/{id}/profile
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
        try {
            UserProfileDTO profile = userService.getUserProfile(id);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user profile for viewer (respects privacy settings)
     * GET /api/users/{id}/profile?viewerId={viewerId}
     */
    @GetMapping("/{id}/profile/view")
    public ResponseEntity<UserProfileDTO> getUserProfileForViewer(
            @PathVariable Long id,
            @RequestParam Long viewerId) {
        try {
            UserProfileDTO profile = userService.getUserProfileForViewer(id, viewerId);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update user profile (legacy endpoint)
     * PUT /api/users/{id}/profile
     */
    @PutMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        try {
            UserProfileDTO updated = userService.updateUserProfile(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload avatar as file
     * POST /api/users/{id}/avatar
     */
    @PostMapping("/{id}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = userService.uploadAvatar(id, file);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IOException | RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload avatar as Base64
     * POST /api/users/{id}/avatar/base64
     */
    @PostMapping("/{id}/avatar/base64")
    public ResponseEntity<Map<String, String>> uploadAvatarBase64(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String base64Image = request.get("avatar");
            if (base64Image == null || base64Image.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            String avatarUrl = userService.uploadAvatarBase64(id, base64Image);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IOException | RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete avatar
     * DELETE /api/users/{id}/avatar
     */
    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<Void> deleteAvatar(@PathVariable Long id) {
        try {
            userService.deleteAvatar(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update privacy settings
     * PUT /api/users/{id}/privacy
     */
    @PutMapping("/{id}/privacy")
    public ResponseEntity<PrivacySettingsDTO> updatePrivacySettings(
            @PathVariable Long id,
            @RequestBody PrivacySettingsDTO request) {
        try {
            PrivacySettingsDTO settings = userService.updatePrivacySettings(id, request);
            return ResponseEntity.ok(settings);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user statistics
     * GET /api/users/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsDTO> getUserStats(@PathVariable Long id) {
        try {
            UserStatsDTO stats = userService.getUserStats(id);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update online status
     * PUT /api/users/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOnlineStatus(
            @PathVariable Long id,
            @RequestParam boolean isOnline) {
        userService.updateOnlineStatus(id, isOnline);
        return ResponseEntity.ok().build();
    }

    /**
     * Update profile background
     * PUT /api/users/{id}/background
     */
    @PutMapping("/{id}/background")
    public ResponseEntity<Map<String, String>> updateBackground(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String background = request.get("background");
            if (background == null || background.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            String updated = userService.updateProfileBackground(id, background);
            return ResponseEntity.ok(Map.of("profileBackground", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's social links
     * GET /api/users/{id}/social-links
     */
    @GetMapping("/{id}/social-links")
    public ResponseEntity<List<SocialLinkDTO>> getSocialLinks(@PathVariable Long id) {
        try {
            List<SocialLinkDTO> links = userService.getSocialLinks(id);
            return ResponseEntity.ok(links);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Add or update a social link
     * POST /api/users/{id}/social-links
     */
    @PostMapping("/{id}/social-links")
    public ResponseEntity<SocialLinkDTO> addSocialLink(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String platform = request.get("platform");
            String url = request.get("url");
            if (platform == null || url == null || platform.isEmpty() || url.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            SocialLinkDTO link = userService.saveSocialLink(id, platform, url);
            return ResponseEntity.ok(link);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a social link
     * DELETE /api/users/{id}/social-links/{platform}
     */
    @DeleteMapping("/{id}/social-links/{platform}")
    public ResponseEntity<Void> deleteSocialLink(
            @PathVariable Long id,
            @PathVariable String platform) {
        try {
            userService.deleteSocialLink(id, platform);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Batch update social links (replace all)
     * PUT /api/users/{id}/social-links
     */
    @PutMapping("/{id}/social-links")
    public ResponseEntity<Map<String, String>> updateSocialLinks(
            @PathVariable Long id,
            @RequestBody Map<String, String> links) {
        try {
            Map<String, String> updated = userService.updateSocialLinks(id, links);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user's recent activities
     * GET /api/users/{id}/activities
     */
    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityDTO>> getUserActivities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<ActivityDTO> activities = userService.getUserActivities(id, limit);
            return ResponseEntity.ok(activities);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get friend activities (activity feed)
     * GET /api/users/{id}/friend-activities
     */
    @GetMapping("/{id}/friend-activities")
    public ResponseEntity<List<ActivityDTO>> getFriendActivities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<ActivityDTO> activities = userService.getFriendActivities(id, limit);
            return ResponseEntity.ok(activities);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
