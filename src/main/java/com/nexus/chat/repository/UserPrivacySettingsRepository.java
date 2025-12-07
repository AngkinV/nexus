package com.nexus.chat.repository;

import com.nexus.chat.model.UserPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, Long> {

    Optional<UserPrivacySettings> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}
