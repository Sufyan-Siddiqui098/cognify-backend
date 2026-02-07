package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recall")
@RequiredArgsConstructor
public class RecallController {

    /**
     * Placeholder for semantic search endpoint
     * Will be implemented when Python service is ready
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(@RequestBody Map<String, Object> request) {
        Map<String, Object> placeholderResponse = new HashMap<>();
        placeholderResponse.put("message", "Recall service is not yet integrated. This endpoint will perform semantic search once the Python service is ready.");
        placeholderResponse.put("query", request.get("query"));
        placeholderResponse.put("results", new Object[]{});
        placeholderResponse.put("status", "pending_python_service");

        return ResponseEntity.ok(ApiResponse.success(placeholderResponse,
                "Python service integration pending"));
    }

    /**
     * Placeholder for feedback endpoint
     */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<ApiResponse<Map<String, String>>> submitFeedback(
            @PathVariable String id,
            @RequestBody Map<String, Object> feedback) {

        Map<String, String> response = new HashMap<>();
        response.put("message", "Feedback endpoint will be active once Python service is integrated");
        response.put("resultId", id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
