package com.cognify.cognify_backend.repository;

import com.cognify.cognify_backend.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    Page<Document> findByUserIdAndStatusNot(UUID userId, Document.DocumentStatus status, Pageable pageable);
    Optional<Document> findByIdAndUserIdAndStatusNot(UUID id, UUID userId, Document.DocumentStatus status);
    long countByUserIdAndStatusNot(UUID userId, Document.DocumentStatus status);
    long countByStatusNot(Document.DocumentStatus status);
    Page<Document> findByStatusNot(Document.DocumentStatus status, Pageable pageable);
    List<Document> findByStatusNot(Document.DocumentStatus status);
}
