package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.AuthResponse;
import com.cognify.cognify_backend.dto.RefreshTokenRequest;
import com.cognify.cognify_backend.config.JwtConfig;
import com.cognify.cognify_backend.entity.User;
import com.cognify.cognify_backend.entity.UserSession;
import com.cognify.cognify_backend.exception.BadRequestException;
import com.cognify.cognify_backend.repository.UserRepository;
import com.cognify.cognify_backend.repository.UserSessionRepository;
import com.cognify.cognify_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtConfig.setAccessExpiration(900000L);
        jwtConfig.setRefreshExpiration(604800000L);
        JwtTokenProvider realTokenProvider = new JwtTokenProvider(jwtConfig);

        authService = new AuthService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                realTokenProvider,
                authenticationManager
        );

        user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .name("Alice")
                .passwordHash("encoded")
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
    }

    @Test
    void refreshTokenRotationIssuesNewTokensAndDeletesOldSession() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("old-refresh")
                .build();

        UserSession existingSession = UserSession.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .refreshToken("old-refresh")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(sessionRepository.findByRefreshToken("old-refresh")).thenReturn(Optional.of(existingSession));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();

        verify(sessionRepository, times(1)).delete(existingSession);

        ArgumentCaptor<UserSession> newSessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository, times(1)).save(newSessionCaptor.capture());
        assertThat(newSessionCaptor.getValue().getRefreshToken()).isEqualTo(response.getRefreshToken());
    }

    @Test
    void refreshTokenWithExpiredSessionThrowsBadRequestAndDeletesSession() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        UserSession expiredSession = UserSession.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .refreshToken("expired-token")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(sessionRepository.findByRefreshToken("expired-token")).thenReturn(Optional.of(expiredSession));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");

        verify(sessionRepository).delete(expiredSession);
    }
}
