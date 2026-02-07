package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.dto.ApiResponse;
import com.cognify.cognify_backend.dto.DocumentDto;
import com.cognify.cognify_backend.security.UserPrincipal;
import com.cognify.cognify_backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        DocumentDto document = documentService.uploadDocument(file, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(document, "Document uploaded successfully. Processing has started."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DocumentDto>>> getUserDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Page<DocumentDto> documents = documentService.getUserDocuments(userPrincipal, page, limit, sortBy, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDto>> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        DocumentDto document = documentService.getDocument(id, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        documentService.deleteDocument(id, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiResponse<DocumentDto>> reindexDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        DocumentDto document = documentService.reindexDocument(id, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(document, "Document reindexing started"));
    }
}
