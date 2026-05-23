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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final StorageConfig storageConfig;
    private Path resolvedBasePath;

    @PostConstruct
    public void init() {
        try {
            resolvedBasePath = Paths.get(storageConfig.getBasePath()).toAbsolutePath().normalize();
            if (!Files.exists(resolvedBasePath)) {
                Files.createDirectories(resolvedBasePath);
                log.info("Created base storage directory: {}", resolvedBasePath);
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
            Path userDirectory = resolvedBasePath.resolve(userId.toString());
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
            Path targetLocation = userDirectory.resolve(uniqueFilename).toAbsolutePath().normalize();
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
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            Files.deleteIfExists(path);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw new FileStorageException("Could not delete file: " + filePath, e);
        }
    }

    public byte[] readFile(String filePath) {
        try {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
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

        validateFileSignature(file, contentType);
    }

    private void validateFileSignature(MultipartFile file, String contentType) {
        byte[] header = new byte[16];
        int read;

        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException e) {
            throw new FileStorageException("Failed to inspect uploaded file signature", e);
        }

        if (read <= 0) {
            throw new FileStorageException("Uploaded file appears to be empty or unreadable");
        }

        byte[] prefix = Arrays.copyOf(header, read);
        boolean valid;

        switch (contentType) {
            case "application/pdf" -> valid = isPdf(prefix);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> valid = isZip(prefix);
            case "text/plain" -> valid = isLikelyText(prefix);
            default -> valid = false;
        }

        if (!valid) {
            throw new FileStorageException("File content does not match declared type");
        }
    }

    private boolean isPdf(byte[] prefix) {
        return prefix.length >= 5
                && prefix[0] == '%'
                && prefix[1] == 'P'
                && prefix[2] == 'D'
                && prefix[3] == 'F'
                && prefix[4] == '-';
    }

    private boolean isZip(byte[] prefix) {
        if (prefix.length < 4) {
            return false;
        }
        return prefix[0] == 'P'
                && prefix[1] == 'K'
                && (prefix[2] == 3 || prefix[2] == 5 || prefix[2] == 7)
                && (prefix[3] == 4 || prefix[3] == 6 || prefix[3] == 8);
    }

    private boolean isLikelyText(byte[] prefix) {
        for (byte b : prefix) {
            int value = b & 0xFF;
            if (value == 9 || value == 10 || value == 13) {
                continue;
            }
            if (value < 32 || value == 127) {
                return false;
            }
        }
        return true;
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
