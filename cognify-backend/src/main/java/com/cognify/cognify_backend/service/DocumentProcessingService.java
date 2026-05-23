package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.config.AiServiceConfig;
import com.cognify.cognify_backend.entity.Chunk;
import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.repository.ChunkRepository;
import com.cognify.cognify_backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Async document processing service.
 * Calls the Python AI microservice to extract text, chunk, embed, and store vectors.
 * Saves chunk metadata to PostgreSQL for reference by the RAG pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;
    private final AiServiceConfig aiServiceConfig;

    /**
     * Process document asynchronously via the Python AI microservice.
     * Pipeline: file → Python (extract → chunk → embed → Qdrant) → save chunk metadata to DB.
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> processDocumentAsync(UUID documentId) {
        log.info("Starting async processing for document: {}", documentId);

        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

            log.info("Calling Python AI Service for document processing");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Path resolvedPath = Paths.get(document.getStoragePath());
            if (!resolvedPath.isAbsolute()) {
                resolvedPath = Paths.get("").toAbsolutePath().resolve(resolvedPath).normalize();
            }
            String absolutePath = resolvedPath.toString();

            ProcessDocumentPayload requestBody = new ProcessDocumentPayload(
                document.getId().toString(),
                absolutePath,
                document.getUserId().toString(),
                document.getType().name()
            );
            HttpEntity<ProcessDocumentPayload> request = new HttpEntity<>(requestBody, headers);

            String processUrl = aiServiceConfig.getUrl() + "/api/v1/process-document";
            log.info("Sending request to Python API: {}", processUrl);
            log.debug("Process payload documentId={}, userId={}, type={}, filePath={} ",
                requestBody.document_id(), requestBody.user_id(), requestBody.document_type(), requestBody.file_path());

            ResponseEntity<Map> response = restTemplate.postForEntity(processUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                boolean success = (Boolean) body.get("success");
                if (success) {
                    Number totalChunksNumber = (Number) body.get("total_chunks");
                    int totalChunks = totalChunksNumber != null ? totalChunksNumber.intValue() : 0;
                    log.info("Python service processed document into {} chunks", totalChunks);

                    List<Map<String, Object>> chunksListData = (List<Map<String, Object>>) body.get("chunks");
                    saveChunks(document.getId(), chunksListData);

                    // Update document status to INDEXED
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

            log.info("Document processing completed: {}", documentId);

        } catch (HttpStatusCodeException e) {
            log.error("AI service returned {} for document {}. Response body: {}",
                    e.getStatusCode(), documentId, e.getResponseBodyAsString(), e);
            markDocumentAsFailed(documentId, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
            markDocumentAsFailed(documentId, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Delete vectors from Qdrant via Python AI service.
     * Called when a user deletes a document to clean up orphaned vectors.
     */
    public void deleteVectorsForDocument(UUID documentId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

                DeleteVectorsPayload requestBody = new DeleteVectorsPayload(
                    documentId.toString(),
                    userId.toString()
                );
                HttpEntity<DeleteVectorsPayload> request = new HttpEntity<>(requestBody, headers);

            String deleteUrl = aiServiceConfig.getUrl() + "/api/v1/delete-vectors";
            log.info("Deleting vectors for document: {}", documentId);
                log.debug("Delete payload documentId={}, userId={}", requestBody.document_id(), requestBody.user_id());

            restTemplate.postForEntity(deleteUrl, request, Map.class);
            log.info("Vectors deleted for document: {}", documentId);
        } catch (HttpStatusCodeException e) {
            log.warn("Failed to delete vectors from Qdrant for document: {}. AI response {} body={}",
                    documentId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            // Non-fatal: we still proceed with DB deletion
        } catch (Exception e) {
            log.warn("Failed to delete vectors from Qdrant for document: {}. Vectors may be orphaned.", documentId, e);
            // Non-fatal: we still proceed with DB deletion
        }
    }

    @Transactional
    protected void saveChunks(UUID documentId, List<Map<String, Object>> chunksData) {
        List<Chunk> chunkEntities = new ArrayList<>();

        for (Map<String, Object> data : chunksData) {
            String chunkIdStr = (String) data.get("chunk_id");
            String content = (String) data.get("content");
            Number chunkIndexNum = (Number) data.get("chunk_index");
            Number pageNumNum = (Number) data.get("page_number");
            Number tokenCountNum = (Number) data.get("token_count");

            int chunkIndex = chunkIndexNum != null ? chunkIndexNum.intValue() : 0;
            int pageNum = pageNumNum != null ? pageNumNum.intValue() : 1;
            int tokenCount = tokenCountNum != null ? tokenCountNum.intValue() : 0;

            Chunk chunk = Chunk.builder()
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
        log.info("Saved {} chunks to database", chunkEntities.size());
    }

    /**
     * Mark a document as failed after processing error.
     * Uses a separate transaction via self-invocation pattern.
     */
    @Transactional
    protected void markDocumentAsFailed(UUID documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(Document.DocumentStatus.FAILED);
            document.setSummary("Processing failed: " + errorMessage);
            documentRepository.save(document);
        });
    }

    private record ProcessDocumentPayload(
            String document_id,
            String file_path,
            String user_id,
            String document_type
    ) {}

    private record DeleteVectorsPayload(
            String document_id,
            String user_id
    ) {}
}
