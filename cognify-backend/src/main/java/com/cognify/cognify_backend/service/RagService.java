package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.MessageDto;
import com.cognify.cognify_backend.dto.SourceChunkDto;
import com.cognify.cognify_backend.entity.Chunk;
import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.entity.Message;
import com.cognify.cognify_backend.entity.MessageSource;
import com.cognify.cognify_backend.repository.ChunkRepository;
import com.cognify.cognify_backend.repository.DocumentRepository;
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

/**
 * RAG (Retrieval-Augmented Generation) Service
 *
 * This service orchestrates the RAG pipeline:
 * 1. Retrieve relevant chunks from vector database
 * 2. Build context from chunks
 * 3. Generate LLM response with context
 * 4. Save response with source citations
 *
 * TODO: Integrate with actual LLM and vector database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final MessageRepository messageRepository;
    private final MessageSourceRepository messageSourceRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    // TODO: Inject VectorStoreService when implemented
    // TODO: Inject LlmService when implemented
    // TODO: Inject EmbeddingService when implemented

    @Transactional
    public MessageDto generateResponse(UUID conversationId, UUID userId, String userMessage) {
        log.info("Generating RAG response for conversation: {}", conversationId);

        try {
            // Step 1: Retrieve relevant chunks (TODO: Use vector database)
            List<Chunk> relevantChunks = retrieveRelevantChunks(userId, userMessage);
            log.info("Retrieved {} relevant chunks", relevantChunks.size());

            // Step 2: Build context from chunks
            String context = buildContext(relevantChunks);

            // Step 3: Get chat history for context (last 5 messages)
            List<Message> chatHistory = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversationId);
            List<Message> recentHistory = chatHistory.stream()
                    .skip(Math.max(0, chatHistory.size() - 5))
                    .collect(Collectors.toList());

            // Step 4: Generate LLM response (TODO: Use actual LLM)
            String assistantResponse = generateLlmResponse(userMessage, context, recentHistory);

            // Step 5: Save assistant message
            Message assistantMessage = Message.builder()
                    .conversationId(conversationId)
                    .sender(Message.MessageSender.ASSISTANT)
                    .content(assistantResponse)
                    .build();
            assistantMessage = messageRepository.save(assistantMessage);

            // Step 6: Save message sources (citations)
            saveMessageSources(assistantMessage.getId(), relevantChunks);

            // Step 7: Return response with sources
            return mapToDto(assistantMessage, relevantChunks);

        } catch (Exception e) {
            log.error("Error in RAG pipeline", e);
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    /**
     * Retrieve relevant chunks from vector database
     *
     * TODO: Replace with actual vector database query
     * Currently returns all chunks as placeholder
     */
    private List<Chunk> retrieveRelevantChunks(UUID userId, String query) {
        log.info("Retrieving relevant chunks for query: {}", query);

        // TODO: Implement actual vector search
        // 1. Generate embedding for query using EmbeddingService
        // 2. Query vector database (Pinecone/Qdrant) with embedding
        // 3. Filter by user_id to only search user's documents
        // 4. Return top K chunks (e.g., K=5) with highest similarity scores

        // PLACEHOLDER: Return limited chunks for now
        List<UUID> userDocumentIds = documentRepository.findByUserId(userId, null)
                .stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        if (userDocumentIds.isEmpty()) {
            log.warn("No documents found for user: {}", userId);
            return new ArrayList<>();
        }

        // Get first document's chunks as placeholder
        UUID firstDocId = userDocumentIds.get(0);
        List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(firstDocId);

        // Return first 5 chunks as mock relevant results
        return chunks.stream()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Build context string from chunks
     */
    private String buildContext(List<Chunk> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant context found.";
        }

        StringBuilder context = new StringBuilder("Based on the following information from your documents:\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);
            String docName = doc != null ? doc.getName() : "Unknown Document";

            context.append(String.format("[Source %d: %s", i + 1, docName));
            if (chunk.getPageNumber() != null) {
                context.append(String.format(", Page %d", chunk.getPageNumber()));
            }
            context.append("]\n");
            context.append(chunk.getContent());
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * Generate LLM response with context
     *
     * TODO: Replace with actual LLM API call (OpenAI, Anthropic, etc.)
     */
    private String generateLlmResponse(String userMessage, String context, List<Message> chatHistory) {
        log.info("Generating LLM response");

        // TODO: Implement actual LLM call
        // 1. Build system prompt for RAG
        // 2. Add chat history for context
        // 3. Add retrieved context
        // 4. Add user message
        // 5. Call LLM API (e.g., OpenAI ChatCompletion)
        // 6. Return generated response

        // PLACEHOLDER: Return mock response
        if (context.contains("No relevant context")) {
            return "I don't have any relevant information in your uploaded documents to answer that question. " +
                   "Could you please upload some documents first, or ask something else?";
        }

        return String.format(
                "Based on your documents, here's what I found:\n\n" +
                "%s\n\n" +
                "To answer your question: %s\n\n" +
                "[This is a placeholder response. Integrate with OpenAI/Anthropic API to generate actual responses.]",
                context.substring(0, Math.min(500, context.length())),
                userMessage
        );
    }

    /**
     * Save message sources (citations) linking message to chunks
     */
    private void saveMessageSources(UUID messageId, List<Chunk> chunks) {
        log.info("Saving message sources for message: {}", messageId);

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);

            // Calculate mock relevance score (in real implementation, comes from vector DB)
            float relevanceScore = 1.0f - (i * 0.1f); // Decreasing scores

            MessageSource source = MessageSource.builder()
                    .messageId(messageId)
                    .chunkId(chunk.getId())
                    .relevanceScore(relevanceScore)
                    .build();

            messageSourceRepository.save(source);
        }

        log.info("Saved {} message sources", chunks.size());
    }

    /**
     * Map Message to DTO with sources
     */
    private MessageDto mapToDto(Message message, List<Chunk> sourceChunks) {
        List<SourceChunkDto> sources = sourceChunks.stream()
                .map(this::mapChunkToSourceDto)
                .collect(Collectors.toList());

        return MessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .sender(message.getSender().name())
                .content(message.getContent())
                .sources(sources)
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * Map Chunk to SourceChunkDto
     */
    private SourceChunkDto mapChunkToSourceDto(Chunk chunk) {
        Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);

        String snippet = chunk.getContent().length() > 200
                ? chunk.getContent().substring(0, 200) + "..."
                : chunk.getContent();

        return SourceChunkDto.builder()
                .chunkId(chunk.getId())
                .documentId(chunk.getDocumentId())
                .documentName(doc != null ? doc.getName() : "Unknown")
                .pageNumber(chunk.getPageNumber())
                .snippet(snippet)
                .relevanceScore(0.85f) // Placeholder
                .build();
    }

    /**
     * Map MessageSource to SourceChunkDto (used by ChatService)
     */
    public SourceChunkDto mapMessageSourceToDto(MessageSource messageSource) {
        Chunk chunk = chunkRepository.findById(messageSource.getChunkId()).orElse(null);
        if (chunk == null) {
            return null;
        }

        Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);

        String snippet = chunk.getContent().length() > 200
                ? chunk.getContent().substring(0, 200) + "..."
                : chunk.getContent();

        return SourceChunkDto.builder()
                .chunkId(chunk.getId())
                .documentId(chunk.getDocumentId())
                .documentName(doc != null ? doc.getName() : "Unknown")
                .pageNumber(chunk.getPageNumber())
                .snippet(snippet)
                .relevanceScore(messageSource.getRelevanceScore())
                .build();
    }
}
