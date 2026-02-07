package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.*;
import com.cognify.cognify_backend.entity.User;
import com.cognify.cognify_backend.entity.UserSession;
import com.cognify.cognify_backend.exception.BadRequestException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Signup request for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("User created successfully: {}", user.getId());

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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

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

        log.info("User logged in successfully: {}", user.getId());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public void logout(UserPrincipal userPrincipal) {
        log.info("Logout request for user: {}", userPrincipal.getId());
        sessionRepository.deleteByUserId(userPrincipal.getId());
    }

    public UserDto getProfile(UserPrincipal userPrincipal) {
        return UserDto.builder()
                .id(userPrincipal.getId())
                .email(userPrincipal.getUsername())
                .name(userPrincipal.getName())
                .role(userPrincipal.getRole())
                .build();
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
}
