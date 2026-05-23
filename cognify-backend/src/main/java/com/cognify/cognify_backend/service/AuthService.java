package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.*;
import com.cognify.cognify_backend.entity.User;
import com.cognify.cognify_backend.entity.UserSession;
import com.cognify.cognify_backend.exception.BadRequestException;
import com.cognify.cognify_backend.exception.ConflictException;
import com.cognify.cognify_backend.repository.UserRepository;
import com.cognify.cognify_backend.repository.UserSessionRepository;
import com.cognify.cognify_backend.security.JwtTokenProvider;
import com.cognify.cognify_backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Authentication service handling user registration, login, logout, and token refresh.
 * Uses JWT access tokens (15 min) + refresh tokens (7 days) with rotation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user account.
     * @throws BadRequestException if email already exists
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Signup request for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        validatePasswordStrength(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("User created successfully: {}", user.getId());

        return createSession(user);
    }

    /**
     * Authenticate user with email/password and create a new session.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getId());
        return createSession(user);
    }

    /**
     * Refresh an expired access token using a valid refresh token.
     * Implements token rotation: old refresh token is invalidated and a new one is issued.
     * @throws BadRequestException if refresh token is invalid or expired
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Token refresh requested");

        UserSession session = sessionRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        // Check if refresh token is expired
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            throw new BadRequestException("Refresh token has expired. Please login again.");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Token rotation: delete old session before creating new one
        sessionRepository.delete(session);

        log.info("Token refreshed for user: {}", user.getId());
        return createSession(user);
    }

    /**
     * Logout the user by invalidating all their sessions.
     */
    @Transactional
    public void logout(UserPrincipal userPrincipal) {
        log.info("Logout request for user: {}", userPrincipal.getId());
        sessionRepository.deleteByUserId(userPrincipal.getId());
    }

    /**
     * Get the authenticated user's profile.
     */
    public UserDto getProfile(UserPrincipal userPrincipal) {
        return UserDto.builder()
                .id(userPrincipal.getId())
                .email(userPrincipal.getUsername())
                .name(userPrincipal.getName())
                .role(userPrincipal.getRole())
                .build();
    }

    /**
     * Update the authenticated user's profile (name only).
     */
    @Transactional
    public UserDto updateProfile(UserPrincipal userPrincipal, Map<String, Object> updates) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (updates != null) {
            Object name = updates.get("name");
            if (name instanceof String value && !value.trim().isEmpty()) {
                user.setName(value.trim());
            }
        }

        User saved = userRepository.save(user);
        return UserDto.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .name(saved.getName())
                .role(saved.getRole().name().toLowerCase())
                .build();
    }

    /**
     * Change the authenticated user's password using their current password.
     */
    @Transactional
    public void changePassword(UserPrincipal userPrincipal, ChangePasswordRequest request) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        validatePasswordStrength(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Create a new session (access + refresh tokens) for a user.
     */
    private AuthResponse createSession(User user) {
        UserPrincipal userPrincipal = new UserPrincipal(user);
        String accessToken = tokenProvider.generateAccessToken(userPrincipal, user.getId(), user.getRole().name());
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // Save refresh token session
        UserSession session = UserSession.builder()
                .userId(user.getId())
                .refreshToken(refreshToken)
                .deviceInfo(new HashMap<>())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        sessionRepository.save(session);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name().toLowerCase())
                .build();

        return AuthResponse.builder()
                .user(userDto)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900L) // 15 minutes in seconds
                .build();
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required");
        }

        boolean hasNumber = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasNumber || !hasSpecial) {
            throw new BadRequestException("Password must include at least one number and one special character");
        }
    }
}
