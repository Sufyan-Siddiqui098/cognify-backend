package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.*;
import com.cognify.cognify_backend.security.UserPrincipal;
import com.cognify.cognify_backend.service.ChatService;
import com.cognify.cognify_backend.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Chat & Conversation Management Controller
 *
 * Handles all chat-related operations:
 * - Create/list/delete conversations
 * - Send/retrieve messages
 * - Get message sources (citations)
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ConversationService conversationService;
    private final ChatService chatService;

    /**
     * Create a new conversation
     *
     * POST /api/conversations
     * Body: {"title": "My Conversation"}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationDto>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Creating conversation for user: {}", userPrincipal.getId());

        ConversationDto conversation = conversationService.createConversation(
                userPrincipal.getId(),
                request.getTitle()
        );

        return ResponseEntity.ok(ApiResponse.success(
                conversation,
                "Conversation created successfully"
        ));
    }

    /**
     * Get all conversations for current user
     *
     * GET /api/conversations?page=0&size=20
     */
    @GetMapping
        public ResponseEntity<ApiResponse<PaginatedResponse<ConversationDto>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching conversations for user: {}", userPrincipal.getId());

        Page<ConversationDto> conversations = conversationService.getUserConversations(
                userPrincipal.getId(),
                page,
                size
        );

        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(conversations)));
    }

    /**
     * Get a specific conversation
     *
     * GET /api/conversations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConversationDto>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching conversation: {} for user: {}", id, userPrincipal.getId());

        ConversationDto conversation = conversationService.getConversation(
                id,
                userPrincipal.getId()
        );

        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    /**
     * Delete a conversation
     *
     * DELETE /api/conversations/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Deleting conversation: {} for user: {}", id, userPrincipal.getId());

        conversationService.deleteConversation(id, userPrincipal.getId());

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Conversation deleted successfully"
        ));
    }

        /**
         * Rename a conversation title.
         *
         * PATCH /api/conversations/{id}
         */
        @PatchMapping("/{id}")
        public ResponseEntity<ApiResponse<ConversationDto>> renameConversation(
                        @PathVariable UUID id,
                        @Valid @RequestBody UpdateConversationRequest request,
                        @AuthenticationPrincipal UserPrincipal userPrincipal) {

                log.info("Renaming conversation: {} for user: {}", id, userPrincipal.getId());

                ConversationDto conversation = conversationService.updateConversationTitle(
                                id,
                                userPrincipal.getId(),
                                request.getTitle()
                );

                return ResponseEntity.ok(ApiResponse.success(conversation, "Conversation renamed successfully"));
        }

    /**
     * Get all messages in a conversation
     *
     * GET /api/conversations/{id}/messages?page=0&size=20
     */
    @GetMapping("/{id}/messages")
        public ResponseEntity<ApiResponse<PaginatedResponse<MessageDto>>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Fetching messages for conversation: {}", id);

        Page<MessageDto> messages = chatService.getConversationMessages(
                id,
                userPrincipal.getId(),
                page,
                size
        );

        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(messages)));
    }

    /**
     * Send a message in a conversation (triggers RAG)
     *
     * POST /api/conversations/{id}/messages
     * Body: {"content": "What is the main topic of the document?"}
     *
     * This endpoint:
     * 1. Saves the user message
     * 2. Triggers RAG pipeline (vector search + LLM generation)
     * 3. Saves assistant response with source citations
     * 4. Returns the assistant response WITH sources included automatically
     *
     * Note: Sources are automatically included in the assistant message response,
     * so no separate API call is needed to fetch them.
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Sending message to conversation: {}", id);

        MessageDto response = chatService.sendMessage(
                id,
                userPrincipal.getId(),
                request.getContent(),
                idempotencyKey
        );

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Message processed successfully"
        ));
    }
}
