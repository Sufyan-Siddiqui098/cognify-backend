package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.config.AiServiceConfig;
import com.cognify.cognify_backend.dto.MessageDto;
import com.cognify.cognify_backend.dto.SourceChunkDto;
import com.cognify.cognify_backend.entity.Chunk;
import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.entity.Message;
import com.cognify.cognify_backend.entity.MessageSource;
import com.cognify.cognify_backend.exception.BadRequestException;
import com.cognify.cognify_backend.repository.ChunkRepository;
import com.cognify.cognify_backend.repository.DocumentRepository;
import com.cognify.cognify_backend.repository.MessageRepository;
import com.cognify.cognify_backend.repository.MessageSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import java.util.Map;
import java.util.HashMap;

/**
 * RAG (Retrieval-Augmented Generation) Service.
 *
 * Orchestrates the full RAG pipeline by communicating with the Python AI microservice:
 * 1. Check if user has uploaded documents (friendly error if not)
 * 2. Request relevant chunks from Python vector search API (with real cosine similarity scores)
 * 3. Build context from retrieved chunks
 * 4. Request LLM response from Python generation API
 * 5. Save assistant response with source citations to PostgreSQL
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private static final int DEFAULT_TOP_K = 5;

    private final MessageRepository messageRepository;
    private final MessageSourceRepository messageSourceRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;
    private final AiServiceConfig aiServiceConfig;

    /**
     * Generate a RAG response for a user message.
     * Checks for uploaded documents first, then performs vector search + LLM generation.
     *
     * @param conversationId The conversation context
     * @param userId The authenticated user's ID
     * @param userMessage The user's natural language query
     * @return MessageDto with AI response and source citations
     * @throws BadRequestException if user has no uploaded documents
     */
    @Transactional
    public MessageDto generateResponse(UUID conversationId, UUID userId, String userMessage) {
        log.info("Generating RAG response for conversation: {}", conversationId);

        // Step 0: Check if user has any documents uploaded
        long documentCount = documentRepository.countByUserIdAndStatusNot(userId, Document.DocumentStatus.DELETED);
        if (documentCount == 0) {
            throw new BadRequestException(
                "You haven't uploaded any documents yet. " +
                "Please upload a PDF, Word document, or WhatsApp chat export to get started."
            );
        }

        try {
            // Step 1: Retrieve relevant chunks via Python AI Service
            SearchResultWithScores searchResult = retrieveRelevantChunks(userId, userMessage);
            List<Chunk> relevantChunks = searchResult.chunks;
            List<Float> relevanceScores = searchResult.scores;
            log.info("Retrieved {} relevant chunks", relevantChunks.size());

            // Step 2: Build context from chunks
            String context = buildContext(relevantChunks);

            // Step 3: Get recent chat history for conversational context
            List<Message> chatHistory = messageRepository
                    .findByConversationIdOrderByCreatedAtAsc(conversationId);
            List<Message> recentHistory = chatHistory.stream()
                    .skip(Math.max(0, chatHistory.size() - 5))
                    .collect(Collectors.toList());

            // Step 4: Generate LLM response via Python AI Service (graceful degradation)
            String assistantResponse = generateLlmResponse(userMessage, context, recentHistory);

            // Step 5: Save assistant message
            Message assistantMessage = Message.builder()
                    .conversationId(conversationId)
                    .sender(Message.MessageSender.ASSISTANT)
                    .content(assistantResponse)
                    .build();
            assistantMessage = messageRepository.save(assistantMessage);

            // Step 6: Save message sources with real relevance scores
            saveMessageSources(assistantMessage.getId(), relevantChunks, relevanceScores);

            // Step 7: Return response with sources
            return mapToDto(assistantMessage, relevantChunks, relevanceScores);

        } catch (BadRequestException e) {
            throw e; // Re-throw user-friendly errors
        } catch (Exception e) {
            log.error("Error in RAG pipeline", e);
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    /**
     * Internally bundles chunk entities with their Qdrant cosine similarity scores.
     */
    private static class SearchResultWithScores {
        List<Chunk> chunks;
        List<Float> scores;

        SearchResultWithScores(List<Chunk> chunks, List<Float> scores) {
            this.chunks = chunks;
            this.scores = scores;
        }
    }

    /**
     * Retrieve relevant chunks from Qdrant via Python AI Service.
     * Filters results by user_id for data isolation.
     */
    private SearchResultWithScores retrieveRelevantChunks(UUID userId, String query) {
        log.info("Retrieving relevant chunks for query: {}", query);

        if (query == null || query.isBlank()) {
            throw new BadRequestException("Query cannot be empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("user_id", userId.toString());
        requestBody.put("top_k", DEFAULT_TOP_K);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String searchUrl = aiServiceConfig.getUrl() + "/api/v1/search";
            ResponseEntity<Map> response = restTemplate.postForEntity(searchUrl, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Retrieval service returned an invalid response.");
            }

            Object resultsObj = response.getBody().get("results");
            if (!(resultsObj instanceof List<?> results)) {
                throw new RuntimeException("Retrieval service contract mismatch: results must be an array.");
            }

            List<Chunk> chunks = new ArrayList<>();
            List<Float> scores = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (Object resultObj : results) {
                if (!(resultObj instanceof Map<?, ?> res)) {
                    log.warn("Skipping malformed search result item type: {}", resultObj);
                    continue;
                }

                String chunkIdStr = asString(res.get("chunk_id"));
                String documentIdStr = asString(res.get("document_id"));
                float score = asFloat(res.get("relevance_score"));

                if (chunkIdStr == null || documentIdStr == null) {
                    log.warn("Skipping malformed search result from AI service. chunk_id={}, document_id={}",
                            chunkIdStr, documentIdStr);
                    continue;
                }

                String dedupeKey = documentIdStr + ":" + chunkIdStr;
                if (!seen.add(dedupeKey)) {
                    continue;
                }

                UUID documentId;
                try {
                    documentId = UUID.fromString(documentIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping AI result with invalid document_id: {}", documentIdStr);
                    continue;
                }

                chunkRepository.findFirstByVectorIdAndDocumentId(chunkIdStr, documentId)
                        .or(() -> chunkRepository.findFirstByVectorIdOrderByCreatedAtDesc(chunkIdStr))
                        .ifPresentOrElse(chunk -> {
                            chunks.add(chunk);
                            scores.add(score);
                        }, () -> log.debug("No chunk row found for AI chunk_id={} and document_id={}",
                                chunkIdStr, documentIdStr));
            }

            if (results.size() > 0 && chunks.isEmpty()) {
                log.warn("AI returned {} results but none could be mapped to local chunks", results.size());
            }

            return new SearchResultWithScores(chunks, scores);
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.error("Python search API failed with status={} body={}", e.getStatusCode(), body, e);
            throw new RuntimeException("Retrieval service failed. Please try again in a moment.");
        } catch (Exception e) {
            log.error("Error calling Python search API", e);
            throw new RuntimeException("Retrieval service is unavailable.", e);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private float asFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof String text) {
            try {
                return Float.parseFloat(text);
            } catch (NumberFormatException ignored) {
                return 0.0f;
            }
        }
        return 0.0f;
    }

    /**
     * Build context string from retrieved chunks for the LLM prompt.
     */
    private String buildContext(List<Chunk> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant context found in the user's documents.";
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
     * Generate LLM response via Ollama through Python AI Service.
     * Gracefully degrades: if Ollama/Python is unavailable, returns raw context summary.
     */
    private String generateLlmResponse(String userMessage, String context, List<Message> chatHistory) {
        log.info("Generating LLM response");

        if (context.contains("No relevant context")) {
            return "I couldn't find relevant information in your uploaded documents to answer that question. " +
                   "Try rephrasing your question, or upload additional documents that may contain the answer.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_message", userMessage);
        requestBody.put("context", context);

        List<Map<String, String>> historyList = new ArrayList<>();
        for (Message m : chatHistory) {
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", m.getSender() == Message.MessageSender.USER ? "user" : "assistant");
            msgMap.put("content", m.getContent());
            historyList.add(msgMap);
        }
        requestBody.put("chat_history", historyList);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        try {
            String generateUrl = aiServiceConfig.getUrl() + "/api/v1/rag/generate";
            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            log.warn("LLM generation unavailable (Ollama may be down). Returning raw chunks as fallback.", e);
            // Graceful degradation: return the context directly without LLM synthesis
            return "I found relevant information in your documents, but the AI summary service is temporarily unavailable.\n\n" + context;
        }

        return "I'm sorry, I couldn't generate a response at this time. Please try again.";
    }

    /**
     * Save message sources (citations) with real relevance scores from Qdrant.
     */
    private void saveMessageSources(UUID messageId, List<Chunk> chunks, List<Float> scores) {
        log.info("Saving {} message sources for message: {}", chunks.size(), messageId);

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            float score = i < scores.size() ? scores.get(i) : 0.0f;
            Document doc = documentRepository.findById(chunk.getDocumentId()).orElse(null);

            String snippet = chunk.getContent().length() > 200
                    ? chunk.getContent().substring(0, 200) + "..."
                    : chunk.getContent();

            MessageSource source = MessageSource.builder()
                    .messageId(messageId)
                    .chunkId(chunk.getId())
                    .documentId(chunk.getDocumentId())
                    .documentName(doc != null ? doc.getName() : "Unknown")
                    .pageNumber(chunk.getPageNumber())
                    .snippet(snippet)
                    .relevanceScore(score)
                    .build();

            messageSourceRepository.save(source);
        }
    }

    /**
     * Map Message to DTO with real source chunks and relevance scores.
     */
    private MessageDto mapToDto(Message message, List<Chunk> sourceChunks, List<Float> scores) {
        List<SourceChunkDto> sources = new ArrayList<>();
        for (int i = 0; i < sourceChunks.size(); i++) {
            Chunk chunk = sourceChunks.get(i);
            float score = i < scores.size() ? scores.get(i) : 0.0f;
            sources.add(mapChunkToSourceDto(chunk, score));
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

    /**
     * Map Chunk entity to SourceChunkDto with document metadata.
     */
    private SourceChunkDto mapChunkToSourceDto(Chunk chunk, float relevanceScore) {
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
                .relevanceScore(relevanceScore)
                .build();
    }

    /**
     * Map MessageSource entity to SourceChunkDto (used by ChatService for historical messages).
     */
    public SourceChunkDto mapMessageSourceToDto(MessageSource messageSource) {
        Chunk chunk = null;
        if (messageSource.getChunkId() != null) {
            chunk = chunkRepository.findById(messageSource.getChunkId()).orElse(null);
        }

        if (chunk != null) {
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

        // Historical fallback: preserve citation display even after chunk reindex/delete.
        return SourceChunkDto.builder()
            .chunkId(messageSource.getChunkId())
            .documentId(messageSource.getDocumentId())
            .documentName(messageSource.getDocumentName() != null ? messageSource.getDocumentName() : "Unknown")
            .pageNumber(messageSource.getPageNumber())
            .snippet(messageSource.getSnippet())
            .relevanceScore(messageSource.getRelevanceScore())
            .build();
    }
}
