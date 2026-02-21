package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.ConversationDto;
import com.cognify.cognify_backend.entity.Conversation;
import com.cognify.cognify_backend.exception.ResourceNotFoundException;
import com.cognify.cognify_backend.repository.ConversationRepository;
import com.cognify.cognify_backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ConversationDto createConversation(UUID userId, String title) {
        log.info("Creating conversation for user: {}", userId);

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(title)
                .build();

        conversation = conversationRepository.save(conversation);
        log.info("Created conversation: {}", conversation.getId());

        return mapToDto(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationDto> getUserConversations(UUID userId, int page, int size) {
        log.info("Fetching conversations for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Conversation> conversations = conversationRepository.findByUserId(userId, pageable);

        return conversations.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversation(UUID conversationId, UUID userId) {
        log.info("Fetching conversation: {} for user: {}", conversationId, userId);

        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        return mapToDto(conversation);
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        log.info("Deleting conversation: {} for user: {}", conversationId, userId);

        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        conversationRepository.delete(conversation);
        log.info("Deleted conversation: {}", conversationId);
    }

    @Transactional
    public ConversationDto updateConversationTitle(UUID conversationId, UUID userId, String newTitle) {
        log.info("Updating conversation title: {}", conversationId);

        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        conversation.setTitle(newTitle);
        conversation = conversationRepository.save(conversation);

        return mapToDto(conversation);
    }

    private ConversationDto mapToDto(Conversation conversation) {
        long messageCount = messageRepository.countByConversationId(conversation.getId());

        return ConversationDto.builder()
                .id(conversation.getId())
                .userId(conversation.getUserId())
                .title(conversation.getTitle())
                .messageCount((int) messageCount)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
