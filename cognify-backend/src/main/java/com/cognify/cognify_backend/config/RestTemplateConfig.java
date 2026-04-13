package com.cognify.cognify_backend.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * RestTemplate configuration for inter-service communication.
 * Provides a centralized, reusable RestTemplate bean with
 * sensible timeouts for calling the Python AI microservice.
 */
@Configuration
@Slf4j
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
    requestFactory.setReadTimeout((int) Duration.ofMinutes(3).toMillis());

    RestTemplate restTemplate = builder
        .requestFactory(() -> requestFactory)
        .build();

    log.info("Configured RestTemplate requestFactory={}", restTemplate.getRequestFactory().getClass().getName());

    return restTemplate;
    }
}
