package com.nexus.chat.repository;

import com.nexus.chat.model.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    
    Optional<FileUpload> findByMessageId(Long messageId);
    
}
