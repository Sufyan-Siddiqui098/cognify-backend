package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.entity.User;
import com.cognify.cognify_backend.exception.BadRequestException;
import com.cognify.cognify_backend.exception.ResourceNotFoundException;
import com.cognify.cognify_backend.repository.DocumentRepository;
import com.cognify.cognify_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin service for platform management operations.
 * All operations require ADMIN role (enforced at controller level via SecurityConfig).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final AnalyticsService analyticsService;
    private final Map<String, Object> systemSettings = new ConcurrentHashMap<>();

    /**
     * Get paginated list of all users with their document counts.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> usersPage = userRepository.findAll(pageable);

        var users = usersPage.getContent().stream().map(this::mapUserSummary).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", users);
        result.put("totalUsers", usersPage.getTotalElements());
        result.put("totalPages", usersPage.getTotalPages());
        result.put("currentPage", page);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapUserSummary(user);
    }

    @Transactional
    public Map<String, Object> updateUser(UUID userId, Map<String, Object> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (updates == null || updates.isEmpty()) {
            return mapUserSummary(user);
        }

        Object name = updates.get("name");
        if (name instanceof String value && !value.trim().isEmpty()) {
            user.setName(value.trim());
        }

        Object email = updates.get("email");
        if (email instanceof String value && !value.trim().isEmpty()) {
            String normalized = value.trim().toLowerCase();
            if (!normalized.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(normalized)) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(normalized);
        }

        Object role = updates.get("role");
        if (role instanceof String value && !value.trim().isEmpty()) {
            try {
                user.setRole(User.Role.valueOf(value.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid role value");
            }
        }

        Object status = updates.get("status");
        if (status instanceof String value && !value.trim().isEmpty()) {
            try {
                user.setStatus(User.Status.valueOf(value.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid status value");
            }
        }

        User saved = userRepository.save(user);
        return mapUserSummary(saved);
    }

    /**
     * Activate a user account.
     * @throws BadRequestException if user is an admin (admins can't be deactivated/reactivated)
     */
    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(User.Status.ACTIVE);
        userRepository.save(user);
        log.info("User activated: {}", userId);
    }

    /**
     * Deactivate a user account. Prevents login but preserves data.
     * @throws BadRequestException if user is an admin
     */
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new BadRequestException("Cannot deactivate admin users");
        }
        user.setStatus(User.Status.INACTIVE);
        userRepository.save(user);
        log.info("User deactivated: {}", userId);
    }

    /**
     * Set user status from a single boolean flag used by the unified admin endpoint.
     */
    @Transactional
    public void setUserStatus(UUID userId, Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            activateUser(userId);
            return;
        }
        deactivateUser(userId);
    }

    /**
     * Delete a user account. Cannot delete admin users.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new BadRequestException("Cannot delete admin users");
        }
        userRepository.delete(user);
        log.info("User deleted: {}", userId);
    }

    /**
     * Get paginated list of all documents across all users.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAllDocuments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Document> docsPage = documentRepository.findByStatusNot(Document.DocumentStatus.DELETED, pageable);

        var documents = docsPage.getContent().stream().map(doc -> {
            User owner = userRepository.findById(doc.getUserId()).orElse(null);
            Map<String, Object> docMap = new LinkedHashMap<>();
            docMap.put("id", doc.getId());
            docMap.put("name", doc.getName());
            docMap.put("type", doc.getType().name());
            docMap.put("size", doc.getSize());
            docMap.put("status", doc.getStatus().name().toLowerCase());
            docMap.put("chunksCount", doc.getChunksCount());
            docMap.put("ownerName", owner != null ? owner.getName() : "Unknown");
            docMap.put("ownerEmail", owner != null ? owner.getEmail() : "Unknown");
            docMap.put("createdAt", doc.getCreatedAt());
            return docMap;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documents", documents);
        result.put("totalDocuments", docsPage.getTotalElements());
        result.put("totalPages", docsPage.getTotalPages());
        result.put("currentPage", page);
        return result;
    }

    /**
     * Get platform-wide analytics for the admin dashboard.
     */
    public Map<String, Object> getPlatformAnalytics() {
        return analyticsService.getPlatformAnalytics();
    }

    /**
     * Get usage timeline for admin analytics charts.
     */
    public Map<String, Object> getUsageTimeline() {
        return analyticsService.getUsageTimeline();
    }

    /**
     * Returns mutable platform settings for admin management UI.
     */
    public Map<String, Object> getSystemSettings() {
        if (systemSettings.isEmpty()) {
            systemSettings.put("maintenanceMode", false);
            systemSettings.put("maxUploadSizeMb", 50);
            systemSettings.put("defaultSearchTopK", 5);
        }
        return new HashMap<>(systemSettings);
    }

    /**
     * Updates mutable platform settings in-memory.
     */
    public Map<String, Object> updateSystemSettings(Map<String, Object> updates) {
        getSystemSettings();
        if (updates != null) {
            systemSettings.putAll(updates);
        }
        return new HashMap<>(systemSettings);
    }

    private Map<String, Object> mapUserSummary(User user) {
        long docCount = documentRepository.countByUserIdAndStatusNot(user.getId(), Document.DocumentStatus.DELETED);
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole().name().toLowerCase());
        userMap.put("status", user.getStatus().name().toLowerCase());
        userMap.put("documentCount", docCount);
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("lastLoginAt", user.getLastLoginAt());
        return userMap;
    }
}
