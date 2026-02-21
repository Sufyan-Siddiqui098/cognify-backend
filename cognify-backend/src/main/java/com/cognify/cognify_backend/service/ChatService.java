package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.MessageDto;
import com.cognify.cognify_backend.dto.SourceChunkDto;
import com.cognify.cognify_backend.entity.Message;
import com.cognify.cognify_backend.entity.MessageSource;
import com.cognify.cognify_backend.exception.ResourceNotFoundException;
import com.cognify.cognify_backend.repository.ConversationRepository;
import com.cognify.cognify_backend.repository.MessageRepository;
import com.cognify.cognify_backend.repository.MessageSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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

    @Transactional
    public MessageDto sendMessage(UUID conversationId, UUID userId, String content) {
        log.info("Processing message for conversation: {}", conversationId);

        // Verify conversation belongs to user (access control)
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Save user message
        Message userMessage = Message.builder()
                .conversationId(conversationId)
                .sender(Message.MessageSender.USER)
                .content(content)
                .build();
        userMessage = messageRepository.save(userMessage);
        log.info("Saved user message: {}", userMessage.getId());

        // Generate RAG response
        try {
            MessageDto assistantMessage = ragService.generateResponse(conversationId, userId, content);
            log.info("Generated assistant response");
            return assistantMessage;
        } catch (Exception e) {
            log.error("Error generating RAG response", e);

            // Save fallback message
            Message fallbackMessage = Message.builder()
                    .conversationId(conversationId)
                    .sender(Message.MessageSender.ASSISTANT)
                    .content("I apologize, but I'm having trouble processing your request right now. Please try again later.")
                    .build();
            fallbackMessage = messageRepository.save(fallbackMessage);

            return mapToDto(fallbackMessage);
        }
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
