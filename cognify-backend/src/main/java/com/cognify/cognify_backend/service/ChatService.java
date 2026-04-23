package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.MessageDto;
import com.cognify.cognify_backend.dto.SourceChunkDto;
import com.cognify.cognify_backend.entity.Message;
import com.cognify.cognify_backend.entity.MessageSource;
import com.cognify.cognify_backend.exception.ConflictException;
import com.cognify.cognify_backend.exception.ResourceNotFoundException;
import com.cognify.cognify_backend.repository.ConversationRepository;
import com.cognify.cognify_backend.repository.MessageRepository;
import com.cognify.cognify_backend.repository.MessageSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageSourceRepository messageSourceRepository;
    private final RagService ragService;

    @Transactional(readOnly = true)
    public List<MessageDto> getConversationMessages(UUID conversationId, UUID userId) {
        log.info("Fetching messages for conversation: {}", conversationId);

        // Verify conversation belongs to user
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        return messages.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

        @Transactional(readOnly = true)
        public Page<MessageDto> getConversationMessages(UUID conversationId, UUID userId, int page, int size) {
        log.info("Fetching paginated messages for conversation: {} page={} size={}", conversationId, page, size);

        conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);

        List<MessageDto> messageDtos = messagePage.getContent().stream()
            .map(this::mapToDto)
            .toList();

        return new PageImpl<>(messageDtos, pageable, messagePage.getTotalElements());
        }

    @Transactional
    public MessageDto sendMessage(UUID conversationId, UUID userId, String content, String idempotencyKey) {
        log.info("Processing message for conversation: {}", conversationId);

        // Verify conversation belongs to user (access control)
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        String normalizedRequestKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedRequestKey != null) {
            Optional<Message> existingUserMessage = messageRepository
                    .findByConversationIdAndSenderAndClientRequestId(
                            conversationId,
                            Message.MessageSender.USER,
                            normalizedRequestKey
                    );

            if (existingUserMessage.isPresent()) {
                Optional<Message> existingAssistantMessage = messageRepository
                        .findByConversationIdAndSenderAndReplyToMessageId(
                                conversationId,
                                Message.MessageSender.ASSISTANT,
                                existingUserMessage.get().getId()
                        );

                if (existingAssistantMessage.isPresent()) {
                    log.info("Returning idempotent response for request key {}", normalizedRequestKey);
                    return mapToDto(existingAssistantMessage.get());
                }

                throw new ConflictException(
                        "A response for this request is still being generated. Please wait and retry."
                );
            }
        }

        Boolean lockAcquired = messageRepository.tryAcquireConversationLock(conversationLockKey(conversationId));
        if (!Boolean.TRUE.equals(lockAcquired)) {
            throw new ConflictException("Another message is currently being processed in this conversation.");
        }

        // Save user message
        Message userMessage = Message.builder()
                .conversationId(conversationId)
                .sender(Message.MessageSender.USER)
                .content(content)
                .clientRequestId(normalizedRequestKey)
                .build();
        userMessage = messageRepository.save(userMessage);
        log.info("Saved user message: {}", userMessage.getId());

        MessageDto assistantMessage = ragService.generateResponse(conversationId, userId, content, userMessage.getId());
        log.info("Generated assistant response");
        return assistantMessage;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }

        String trimmed = idempotencyKey.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long conversationLockKey(UUID conversationId) {
        return conversationId.getMostSignificantBits() ^ conversationId.getLeastSignificantBits();
    }

    /**
     * Get message sources - used internally to populate assistant message DTOs
     */
    private List<SourceChunkDto> getMessageSources(UUID messageId) {
        log.debug("Fetching sources for message: {}", messageId);

        List<MessageSource> sources = messageSourceRepository.findByMessageIdOrderByRelevanceScoreDesc(messageId);

        return sources.stream()
                .map(ragService::mapMessageSourceToDto)
                .collect(Collectors.toList());
    }

    private MessageDto mapToDto(Message message) {
        List<SourceChunkDto> sources = new ArrayList<>();

        if (message.getSender() == Message.MessageSender.ASSISTANT) {
            sources = getMessageSources(message.getId());
        }

        return MessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .sender(message.getSender().name())
                .content(message.getContent())
                .sources(sources)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
