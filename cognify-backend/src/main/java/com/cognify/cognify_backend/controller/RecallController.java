package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * RecallController — DEPRECATED.
 * 
 * Recall functionality has been merged into the Chat system.
 * Semantic search is now handled through:
 * - POST /api/conversations/{id}/messages (sends query, triggers RAG pipeline)
 * - GET /api/conversations/{id}/messages (retrieves results with source citations)
 *
 * This controller exists only for backwards compatibility with existing bookmarks/links.
 * New clients should use the Chat API.
 */
@RestController
@RequestMapping("/api/recall")
@Deprecated
public class RecallController {

    /**
     * Search endpoint — redirects clients to use Chat API instead.
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Void>> search() {
        return ResponseEntity.ok(ApiResponse.success(null,
                "Recall has been integrated into the Chat system. " +
                "Please use POST /api/conversations/{id}/messages to perform semantic search."));
    }
}
