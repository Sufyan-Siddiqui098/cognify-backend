package com.cognify.cognify_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cognify.jwt")
@Data
public class JwtConfig {
    private String secret;
    private long accessExpiration;
    private long refreshExpiration;
}
