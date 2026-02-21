package com.cognify.cognify_backend.repository;

import com.cognify.cognify_backend.entity.MessageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageSourceRepository extends JpaRepository<MessageSource, UUID> {

    List<MessageSource> findByMessageIdOrderByRelevanceScoreDesc(UUID messageId);

    List<MessageSource> findByChunkId(UUID chunkId);

    @Query("SELECT ms FROM MessageSource ms WHERE ms.messageId = :messageId ORDER BY ms.relevanceScore DESC")
    List<MessageSource> findSourcesByMessage(UUID messageId);

    void deleteByMessageId(UUID messageId);

    void deleteByChunkId(UUID chunkId);
}
