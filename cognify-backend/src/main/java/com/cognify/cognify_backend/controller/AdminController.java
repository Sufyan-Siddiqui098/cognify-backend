package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ApiResponse;
import com.cognify.cognify_backend.dto.AdminUserStatusRequest;
import com.cognify.cognify_backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin Controller — Platform management endpoints.
 * All endpoints require ADMIN role (enforced by SecurityConfig: /api/admin/** -> hasRole("ADMIN")).
 * Provides user management, cross-user document listing, and platform-wide analytics.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Get paginated list of all users with document counts.
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {
        Map<String, Object> result = adminService.getUsers(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get a single user profile for admin inspection.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserById(userId)));
    }

    /**
     * Update a user profile from admin UI.
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUser(
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.updateUser(userId, updates),
                "User updated"
        ));
    }

    /**
     * Activate a user account.
     */
    @PatchMapping("/users/{userId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User activated"));
    }

    /**
     * Deactivate a user account. Cannot deactivate admin users.
     */
    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID userId) {
        adminService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated"));
    }

    /**
     * Update user account status using a single endpoint contract.
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserStatusRequest request) {
        adminService.setUserStatus(userId, request.getActive());
        String statusText = Boolean.TRUE.equals(request.getActive()) ? "active" : "inactive";
        return ResponseEntity.ok(ApiResponse.success(null, "User status updated to " + statusText));
    }

    /**
     * Delete a user account. Cannot delete admin users.
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }

    /**
     * Get paginated list of all documents across all users.
     */
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Map<String, Object> result = adminService.getAllDocuments(page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get platform-wide analytics summary.
     */
    @GetMapping("/analytics/platform")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformAnalytics() {
        Map<String, Object> analytics = adminService.getPlatformAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    /**
     * Get platform usage timeline (user registrations + document uploads over 30 days).
     */
    @GetMapping("/analytics/usage-timeline")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsageTimeline() {
        Map<String, Object> timeline = adminService.getUsageTimeline();
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    /**
     * Get mutable platform settings for admin UI.
     */
    @GetMapping("/system/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemSettings() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSystemSettings()));
    }

    /**
     * Update mutable platform settings for admin UI.
     */
    @PutMapping("/system/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSystemSettings(
            @RequestBody Map<String, Object> settings) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.updateSystemSettings(settings),
                "System settings updated"
        ));
    }
}
