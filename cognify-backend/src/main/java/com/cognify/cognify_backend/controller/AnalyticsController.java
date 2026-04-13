package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ApiResponse;
import com.cognify.cognify_backend.security.UserPrincipal;
import com.cognify.cognify_backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Analytics Controller.
 * Provides user-specific analytics endpoints for the dashboard.
 * All endpoints are authenticated and scoped to the requesting user.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get comprehensive analytics summary for the authenticated user.
     * Returns total documents, chunks, conversations, searches, and storage used.
     */
    @GetMapping("/user/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Map<String, Object> summary = analyticsService.getUserSummary(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Get document type distribution (PDF/DOCX/TXT) for pie chart.
     */
    @GetMapping("/user/document-types")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDocumentTypes(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Map<String, Object>> distribution = analyticsService.getDocumentTypeDistribution(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * Get upload timeline data for line/bar chart (last 30 days).
     */
    @GetMapping("/user/upload-timeline")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUploadTimeline(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Map<String, Object>> timeline = analyticsService.getUploadTimeline(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    /**
     * Get recent activity (last 10 document events).
     */
    @GetMapping("/user/recent-activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Map<String, Object>> activity = analyticsService.getRecentActivity(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    /**
     * Get top documents by searchability (most chunks).
     */
    @GetMapping("/user/top-documents")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopDocuments(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Map<String, Object>> topDocs = analyticsService.getTopDocuments(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(topDocs));
    }
}
