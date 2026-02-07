package com.cognify.cognify_backend.repository;

import com.cognify.cognify_backend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByRefreshToken(String refreshToken);
    void deleteByUserId(UUID userId);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
