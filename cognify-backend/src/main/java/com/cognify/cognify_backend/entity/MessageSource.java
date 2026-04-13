package com.cognify.cognify_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_sources", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"message_id", "chunk_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "snippet", columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "relevance_score")
    private Float relevanceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
