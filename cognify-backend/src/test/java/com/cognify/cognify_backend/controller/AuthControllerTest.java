package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.AuthResponse;
import com.cognify.cognify_backend.dto.LoginRequest;
import com.cognify.cognify_backend.dto.SignupRequest;
import com.cognify.cognify_backend.dto.UserDto;
import com.cognify.cognify_backend.exception.ConflictException;
import com.cognify.cognify_backend.exception.GlobalExceptionHandler;
import com.cognify.cognify_backend.security.JwtAuthenticationFilter;
import com.cognify.cognify_backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerWithValidInputReturns201AndJwt() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(900L)
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .email("alice@example.com")
                        .name("Alice")
                        .role("user")
                        .build())
                .build();

        when(authService.signup(any(SignupRequest.class))).thenReturn(response);

        SignupRequest request = SignupRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("StrongPass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new ConflictException("Email already exists"));

        SignupRequest request = SignupRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("StrongPass123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void loginWithCorrectCredentialsReturns200AndJwt() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(900L)
                .user(UserDto.builder()
                        .id(UUID.randomUUID())
                        .email("alice@example.com")
                        .name("Alice")
                        .role("user")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("StrongPass123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("WrongPass")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }
}
