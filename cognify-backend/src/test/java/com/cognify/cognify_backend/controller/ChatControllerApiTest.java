package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ConversationDto;
import com.cognify.cognify_backend.dto.MessageDto;
import com.cognify.cognify_backend.dto.UpdateConversationRequest;
import com.cognify.cognify_backend.entity.User;
import com.cognify.cognify_backend.exception.BadRequestException;
import com.cognify.cognify_backend.exception.GlobalExceptionHandler;
import com.cognify.cognify_backend.security.CustomUserDetailsService;
import com.cognify.cognify_backend.security.JwtTokenProvider;
import com.cognify.cognify_backend.security.UserPrincipal;
import com.cognify.cognify_backend.service.ChatService;
import com.cognify.cognify_backend.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class ChatControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private ChatService chatService;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private CustomUserDetailsService customUserDetailsService;

        private UsernamePasswordAuthenticationToken authToken() {
                User user = User.builder()
                                .id(UUID.randomUUID())
                                .email("test@example.com")
                                .name("Test User")
                                .passwordHash("encoded")
                                .role(User.Role.USER)
                                .status(User.Status.ACTIVE)
                                .build();

                UserPrincipal principal = new UserPrincipal(user);
                return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        }

    @Test
    void createConversationWithValidPayloadReturns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConversationDto conversation = ConversationDto.builder()
                .id(conversationId)
                .userId(userId)
                .title("Revision Notes")
                .messageCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(conversationService.createConversation(any(), eq("Revision Notes"))).thenReturn(conversation);

        mockMvc.perform(post("/api/conversations")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Revision Notes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Revision Notes"));
    }

    @Test
    void renameConversationWithValidPayloadReturns200() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConversationDto conversation = ConversationDto.builder()
                .id(conversationId)
                .userId(userId)
                .title("Renamed Conversation")
                .messageCount(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(conversationService.updateConversationTitle(any(), any(), eq("Renamed Conversation")))
                .thenReturn(conversation);

        UpdateConversationRequest request = UpdateConversationRequest.builder()
                .title("Renamed Conversation")
                .build();

        mockMvc.perform(patch("/api/conversations/{id}", conversationId)
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Renamed Conversation"));
    }

    @Test
    void getMessagesReturnsPaginatedResponse() throws Exception {
        UUID conversationId = UUID.randomUUID();

        MessageDto message = MessageDto.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sender("USER")
                .content("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatService.getConversationMessages(any(), any(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(message)));

        mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                        .with(authentication(authToken()))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].content").value("Hello"));
    }

    @Test
    void createConversationWithoutDocumentsReturns400() throws Exception {
        when(conversationService.createConversation(any(), eq("Revision Notes")))
                .thenThrow(new BadRequestException("Please upload at least one document before starting a conversation."));

        mockMvc.perform(post("/api/conversations")
                        .with(authentication(authToken()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Revision Notes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("Please upload at least one document before starting a conversation."));
    }

    @Test
    void getConversationsReturnsStablePaginationEnvelope() throws Exception {
        UUID userId = UUID.randomUUID();
        ConversationDto conversation = ConversationDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title("Test Conversation")
                .messageCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(conversationService.getUserConversations(any(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(conversation)));

        mockMvc.perform(get("/api/conversations")
                        .with(authentication(authToken()))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Test Conversation"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.number").value(0));
    }
}
