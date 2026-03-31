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
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

/**
 * RAG (Retrieval-Augmented Generation) Service
 *
 * This service acts as an orchestrator for the RAG pipeline by communicating
 * directly with the dedicated Python AI Microservice (cognify-ai-service).
 * 
 * Pipeline:
 * 1. Request relevant chunks from Python vector search API
 * 2. Build context from chunks
 * 3. Request LLM response from Python generation API
 * 4. Save assistant response with source citations securely in DB
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final MessageRepository messageRepository;
    private final MessageSourceRepository messageSourceRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public MessageDto generateResponse(UUID conversationId, UUID userId, String userMessage) {
        log.info("Generating RAG response for conversation: {}", conversationId);

        try {
            // Step 1: Request relevant chunks from Python AI Service
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

            // Step 4: Request LLM generation from Python AI Service
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
     * Retrieve relevant chunks from vector database via Python API
     */
    private List<Chunk> retrieveRelevantChunks(UUID userId, String query) {
        log.info("Retrieving relevant chunks for query: {}", query);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("user_id", userId.toString());
        requestBody.put("top_k", 5);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8000/api/v1/search", 
                    request, 
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                List<Chunk> chunks = new ArrayList<>();
                for (Map<String, Object> res : results) {
                    String chunkIdStr = (String) res.get("chunk_id");
                    chunkRepository.findById(UUID.fromString(chunkIdStr)).ifPresent(chunks::add);
                }
                return chunks;
            }
        } catch(Exception e) {
            log.error("Error calling Python search API", e);
        }
        
        return new ArrayList<>();
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
     * Generate LLM response with context via Python API
     */
    private String generateLlmResponse(String userMessage, String context, List<Message> chatHistory) {
        log.info("Generating LLM response");

        if (context.contains("No relevant context")) {
            return "I don't have any relevant information in your uploaded documents to answer that question. " +
                   "Could you please upload some documents first, or ask something else?";
        }

        RestTemplate restTemplate = new RestTemplate();
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
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8000/api/v1/rag/generate", 
                    request, 
                    Map.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("response");
            }
        } catch (Exception e) {
            log.error("Error communicating with Python API for generation", e);
            throw new RuntimeException("Failed to generate response via LLM API");
        }
        
        return "I'm sorry, I couldn't generate a response at this time.";
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
