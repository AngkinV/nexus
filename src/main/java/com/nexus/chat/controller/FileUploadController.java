package com.nexus.chat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        log.debug("文件上传请求: filename={}, size={}", file.getOriginalFilename(), file.getSize());
        try {
            // Create upload directory if it doesn't exist
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, file.getBytes());

            // Return file URL
            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", "/uploads/" + filename);
            response.put("filename", originalFilename);
            response.put("size", String.valueOf(file.getSize()));

            log.info("文件上传成功: filename={}, savedAs={}", originalFilename, filename);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("文件上传失败: filename={}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<Map<String, Object>> uploadChunk(
            @RequestParam("file") MultipartFile chunk,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("fileId") String fileId) {
        log.debug("分片上传请求: fileId={}, chunkIndex={}/{}", fileId, chunkIndex, totalChunks);
        try {
            // Create temp directory for crunks
            File chunksDir = new File(UPLOAD_DIR + "chunks/" + fileId);
            if (!chunksDir.exists()) {
                chunksDir.mkdirs();
            }

            // Save chunk
            Path chunkPath = Paths.get(chunksDir.getPath() + "/chunk_" + chunkIndex);
            Files.write(chunkPath, chunk.getBytes());

            Map<String, Object> response = new HashMap<>();
            response.put("chunkIndex", chunkIndex);
            response.put("uploaded", true);

            // If all chunks uploaded, merge them
            if (chunkIndex == totalChunks - 1) {
                String mergedFilename = mergeChunks(fileId, totalChunks);
                response.put("complete", true);
                response.put("fileUrl", "/uploads/" + mergedFilename);
                log.info("分片上传完成并合并: fileId={}", fileId);
            } else {
                response.put("complete", false);
            }

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("分片上传失败: fileId={}, chunkIndex={}", fileId, chunkIndex, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String mergeChunks(String fileId, int totalChunks) throws IOException {
        String mergedFilename = fileId;
        Path mergedPath = Paths.get(UPLOAD_DIR + mergedFilename);

        // Merge all chunks
        for (int i = 0; i < totalChunks; i++) {
            Path chunkPath = Paths.get(UPLOAD_DIR + "chunks/" + fileId + "/chunk_" + i);
            byte[] chunkData = Files.readAllBytes(chunkPath);
            Files.write(mergedPath, chunkData, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        }

        // Delete chunks directory
        deleteDirectory(new File(UPLOAD_DIR + "chunks/" + fileId));

        return mergedFilename;
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

}
