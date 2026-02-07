package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.dto.DocumentDto;
import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.exception.ResourceNotFoundException;
import com.cognify.cognify_backend.repository.ChunkRepository;
import com.cognify.cognify_backend.repository.DocumentRepository;
import com.cognify.cognify_backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProcessingService processingService;

    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, UserPrincipal userPrincipal) {
        log.info("Upload document request from user: {}", userPrincipal.getId());

        // Store file
        String storagePath = fileStorageService.storeFile(file, userPrincipal.getId());

        // Determine document type
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        Document.DocumentType type = getDocumentType(extension);

        // Create document entity
        Document document = Document.builder()
                .userId(userPrincipal.getId())
                .name(file.getOriginalFilename())
                .type(type)
                .size(file.getSize())
                .status(Document.DocumentStatus.PROCESSING)
                .storagePath(storagePath)
                .uploadStartedAt(LocalDateTime.now())
                .build();

        document = documentRepository.save(document);
        log.info("Document created with ID: {}", document.getId());

        // Trigger async processing
        processingService.processDocumentAsync(document.getId());

        return mapToDto(document);
    }

    public Page<DocumentDto> getUserDocuments(UserPrincipal userPrincipal, int page, int size, String sortBy, String sortOrder) {
        Sort.Direction direction = sortOrder.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Document> documents = documentRepository.findByUserId(userPrincipal.getId(), pageable);
        return documents.map(this::mapToDto);
    }

    public DocumentDto getDocument(UUID documentId, UserPrincipal userPrincipal) {
        Document document = documentRepository.findByIdAndUserId(documentId, userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return mapToDto(document);
    }

    @Transactional
    public void deleteDocument(UUID documentId, UserPrincipal userPrincipal) {
        Document document = documentRepository.findByIdAndUserId(documentId, userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Delete chunks
        chunkRepository.deleteByDocumentId(documentId);

        // Delete file
        fileStorageService.deleteFile(document.getStoragePath());

        // Delete document
        documentRepository.delete(document);

        log.info("Document deleted: {}", documentId);
    }

    @Transactional
    public DocumentDto reindexDocument(UUID documentId, UserPrincipal userPrincipal) {
        Document document = documentRepository.findByIdAndUserId(documentId, userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Reset status
        document.setStatus(Document.DocumentStatus.PROCESSING);
        document.setChunksCount(0);
        document.setIndexedAt(null);
        documentRepository.save(document);

        // Delete existing chunks
        chunkRepository.deleteByDocumentId(documentId);

        // Trigger async processing
        processingService.processDocumentAsync(documentId);

        return mapToDto(document);
    }

    private Document.DocumentType getDocumentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "pdf" -> Document.DocumentType.PDF;
            case "docx" -> Document.DocumentType.DOCX;
            case "txt" -> Document.DocumentType.TXT;
            default -> throw new IllegalArgumentException("Unsupported file type: " + extension);
        };
    }

    private DocumentDto mapToDto(Document document) {
        return DocumentDto.builder()
                .id(document.getId())
                .name(document.getName())
                .type(document.getType().name().toLowerCase())
                .size(document.getSize())
                .status(document.getStatus().name().toLowerCase())
                .chunksCount(document.getChunksCount())
                .summary(document.getSummary())
                .tags(document.getTags())
                .uploadStartedAt(document.getUploadStartedAt())
                .indexedAt(document.getIndexedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
