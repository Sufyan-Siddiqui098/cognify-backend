package com.cognify.cognify_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cognify.storage")
@Data
public class StorageConfig {
    private String basePath;
    private List<String> allowedTypes;
    private long maxFileSize;
}
