package com.cognify.cognify_backend.repository;

import com.cognify.cognify_backend.entity.Message;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);

        java.util.Optional<Message> findByConversationIdAndSenderAndClientRequestId(
            UUID conversationId,
            Message.MessageSender sender,
            String clientRequestId
        );

        java.util.Optional<Message> findByConversationIdAndSenderAndReplyToMessageId(
            UUID conversationId,
            Message.MessageSender sender,
            UUID replyToMessageId
        );

        @Query(value = "SELECT pg_try_advisory_xact_lock(:lockKey)", nativeQuery = true)
        Boolean tryAcquireConversationLock(@Param("lockKey") long lockKey);

    long countByConversationId(UUID conversationId);

    void deleteByConversationId(UUID conversationId);
}
