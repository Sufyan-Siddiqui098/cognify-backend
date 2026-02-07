package com.cognify.cognify_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType type;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PROCESSING;

    @Column(name = "chunks_count")
    private Integer chunksCount = 0;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT[]")
    private String[] tags;

    @Column(name = "upload_started_at", nullable = false)
    private LocalDateTime uploadStartedAt;

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum DocumentType {
        PDF, DOCX, TXT
    }

    public enum DocumentStatus {
        PROCESSING, INDEXED, FAILED
    }
}
