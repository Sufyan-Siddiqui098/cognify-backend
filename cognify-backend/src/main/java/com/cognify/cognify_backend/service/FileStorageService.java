package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.config.StorageConfig;
import com.cognify.cognify_backend.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final StorageConfig storageConfig;

    @PostConstruct
    public void init() {
        try {
            Path basePath = Paths.get(storageConfig.getBasePath());
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("Created base storage directory: {}", basePath);
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not create storage directory", e);
        }
    }

    public String storeFile(MultipartFile file, UUID userId) {
        // Validate file
        validateFile(file);

        try {
            // Create user directory if not exists
            Path userDirectory = Paths.get(storageConfig.getBasePath(), userId.toString());
            if (!Files.exists(userDirectory)) {
                Files.createDirectories(userDirectory);
                log.info("Created user directory: {}", userDirectory);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String sanitizedFilename = sanitizeFilename(FilenameUtils.getBaseName(originalFilename));
            String uniqueFilename = sanitizedFilename + "_" + System.currentTimeMillis() + "." + extension;

            // Store file
            Path targetLocation = userDirectory.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", targetLocation);
            return targetLocation.toString();

        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new FileStorageException("Could not store file " + file.getOriginalFilename(), e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new FileStorageException("Could not delete file: " + filePath, e);
        }
    }

    public byte[] readFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            throw new FileStorageException("Could not read file: " + filePath, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw new FileStorageException("File size exceeds maximum allowed size of " + storageConfig.getMaxFileSize() + " bytes");
        }

        String contentType = file.getContentType();
        if (contentType == null || !storageConfig.getAllowedTypes().contains(contentType)) {
            throw new FileStorageException("File type not allowed. Allowed types: " + storageConfig.getAllowedTypes());
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
