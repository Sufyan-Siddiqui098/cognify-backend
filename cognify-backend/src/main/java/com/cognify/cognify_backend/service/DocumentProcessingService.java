package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.entity.Chunk;
import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.repository.ChunkRepository;
import com.cognify.cognify_backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final FileStorageService fileStorageService;

    /**
     * Process document asynchronously using CompletableFuture
     * This simulates the processing pipeline without Python service for now
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processDocumentAsync(UUID documentId) {
        log.info("Starting async processing for document: {}", documentId);

        return CompletableFuture.runAsync(() -> {
            try {
                Document document = documentRepository.findById(documentId)
                        .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

                log.info("Step 1: Calling Python AI Service for processing");
                
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("document_id", document.getId().toString());
                requestBody.put("file_path", document.getStoragePath());
                requestBody.put("user_id", document.getUserId().toString());
                requestBody.put("document_type", document.getType().name());

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                
                log.info("Sending request to Python API: {}", requestBody);
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            "http://localhost:8000/api/v1/process-document", 
                            request, 
                            Map.class
                    );
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> body = response.getBody();
                        boolean success = (Boolean) body.get("success");
                        if (success) {
                            int totalChunks = (Integer) body.get("total_chunks");
                            log.info("Python service successfully processed document into {} chunks", totalChunks);
                            
                            List<Map<String, Object>> chunksListData = (List<Map<String, Object>>) body.get("chunks");
                            saveChunks(document.getId(), chunksListData);
                            
                            // 2. Update document status
                            document.setStatus(Document.DocumentStatus.INDEXED);
                            document.setChunksCount(totalChunks);
                            document.setIndexedAt(LocalDateTime.now());
                            document.setSummary("Document processed and indexed successfully via Local AI Service.");
                            document.setTags(new String[]{"processed", "local-ai"});
                            documentRepository.save(document);
                        } else {
                            throw new RuntimeException("Python service returned success=false");
                        }
                    } else {
                        throw new RuntimeException("Failed to get successful response from Python API");
                    }
                } catch (Exception e) {
                    log.error("Error communicating with Python API", e);
                    throw e;
                }

                log.info("Document processing completed successfully: {}", documentId);

            } catch (Exception e) {
                log.error("Error processing document: {}", documentId, e);
                markDocumentAsFailed(documentId, e.getMessage());
            }
        });
    }

    private void saveChunks(UUID documentId, List<Map<String, Object>> chunksData) {
        List<com.cognify.cognify_backend.entity.Chunk> chunkEntities = new ArrayList<>();
        
        for (Map<String, Object> data : chunksData) {
            String chunkIdStr = (String) data.get("chunk_id");
            String content = (String) data.get("content");
            Integer chunkIndex = (Integer) data.get("chunk_index");
            Integer pageNum = data.get("page_number") != null ? (Integer) data.get("page_number") : 1;
            Integer tokenCount = data.get("token_count") != null ? (Integer) data.get("token_count") : 0;
            
            com.cognify.cognify_backend.entity.Chunk chunk = com.cognify.cognify_backend.entity.Chunk.builder()
                    .id(UUID.fromString(chunkIdStr))
                    .documentId(documentId)
                    .chunkIndex(chunkIndex)
                    .content(content)
                    .pageNumber(pageNum)
                    .tokenCount(tokenCount)
                    .vectorId(chunkIdStr)
                    .build();
            chunkEntities.add(chunk);
        }
        
        chunkRepository.saveAll(chunkEntities);
    }

    @Transactional
    private void markDocumentAsFailed(UUID documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(Document.DocumentStatus.FAILED);
            document.setSummary("Processing failed: " + errorMessage);
            documentRepository.save(document);
        });
    }
}
