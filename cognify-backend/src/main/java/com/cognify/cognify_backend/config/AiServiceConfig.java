package com.cognify.cognify_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Python AI microservice.
 * Maps to 'cognify.ai-service.*' properties in application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "cognify.ai-service")
@Data
public class AiServiceConfig {

    /** Base URL of the Python AI service (e.g., http://localhost:8000) */
    private String url = "http://localhost:8000";
}
