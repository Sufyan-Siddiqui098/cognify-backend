package com.cognify.cognify_backend.repository;

import com.cognify.cognify_backend.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
    List<Chunk> findByDocumentIdOrderByChunkIndex(UUID documentId);
    long countByDocumentId(UUID documentId);
    void deleteByDocumentId(UUID documentId);
}
