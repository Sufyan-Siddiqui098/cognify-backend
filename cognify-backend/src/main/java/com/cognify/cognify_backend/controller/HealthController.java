package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "cognify-backend");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "0.0.1-SNAPSHOT");

        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
