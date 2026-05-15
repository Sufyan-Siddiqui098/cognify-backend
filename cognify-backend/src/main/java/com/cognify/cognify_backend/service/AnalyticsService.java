package com.cognify.cognify_backend.service;

import com.cognify.cognify_backend.entity.Document;
import com.cognify.cognify_backend.entity.Message;
import com.cognify.cognify_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics service providing user-specific and platform-wide metrics.
 * Queries are performed directly against PostgreSQL for real-time accuracy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * Get a comprehensive analytics summary for a specific user.
     * Includes document counts, chunk counts, conversation counts, storage used.
     */
    public Map<String, Object> getUserSummary(UUID userId) {
        log.info("Generating analytics summary for user: {}", userId);

        List<Document> userDocs = documentRepository.findByUserIdAndStatusNot(
                userId,
                Document.DocumentStatus.DELETED,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        long totalChunks = userDocs.stream()
                .mapToLong(d -> d.getChunksCount() != null ? d.getChunksCount() : 0)
                .sum();

        long totalConversations = conversationRepository.countByUserId(userId);

        // Count total queries sent by user (search queries)
        List<Message> userMessages = messageRepository.findByUserIdAndSender(userId, Message.MessageSender.USER);
        long totalSearches = userMessages.size();

        long storageUsed = userDocs.stream()
                .mapToLong(d -> d.getSize() != null ? d.getSize() : 0)
                .sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalDocuments", userDocs.size());
        summary.put("totalChunks", totalChunks);
        summary.put("totalConversations", totalConversations);
                summary.put("totalSearches", totalSearches);
        summary.put("storageUsed", storageUsed);

                Map<String, Object> recallStats = new LinkedHashMap<>();
                recallStats.put("totalQueries", totalSearches);
                recallStats.put("topTopics", buildTopTopics(userMessages));
                summary.put("recallStats", recallStats);
                summary.put("topTopics", recallStats.get("topTopics"));

        return summary;
    }

        private List<Map<String, Object>> buildTopTopics(List<Message> messages) {
                if (messages == null || messages.isEmpty()) {
                        return Collections.emptyList();
                }

                Set<String> stopwords = new HashSet<>(Arrays.asList(
                        "the", "and", "for", "with", "that", "this", "from", "what", "when", "where",
                        "which", "would", "could", "should", "about", "your", "their", "they", "them",
                        "were", "been", "has", "have", "but", "into", "there", "here", "then", "than",
                        "also", "only", "just", "will", "dont", "does", "doesnt", "did", "didnt", "cant",
                        "cannot", "its", "you", "are", "was", "who", "why", "how", "can"
                ));

                Map<String, Long> counts = new HashMap<>();
                for (Message message : messages) {
                        String content = message.getContent();
                        if (content == null || content.isBlank()) {
                                continue;
                        }

                        String[] tokens = content.toLowerCase().split("[^a-z0-9]+");
                        for (String token : tokens) {
                                if (token.length() < 3 || stopwords.contains(token)) {
                                        continue;
                                }
                                counts.put(token, counts.getOrDefault(token, 0L) + 1L);
                        }
                }

                return counts.entrySet().stream()
                                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                                .limit(5)
                                .map(entry -> {
                                        Map<String, Object> topic = new LinkedHashMap<>();
                                        topic.put("topic", entry.getKey());
                                        topic.put("queries", entry.getValue());
                                        return topic;
                                })
                                .collect(Collectors.toList());
        }

    /**
     * Get document type distribution for chart display (pie chart data).
     * Returns breakdown by PDF/DOCX/TXT.
     */
    public List<Map<String, Object>> getDocumentTypeDistribution(UUID userId) {
        List<Document> userDocs = documentRepository.findByUserIdAndStatusNot(
                userId,
                Document.DocumentStatus.DELETED,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        Map<String, Long> typeCounts = userDocs.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getType().name(),
                        Collectors.counting()
                ));

        long total = userDocs.size();
        List<Map<String, Object>> distribution = new ArrayList<>();
        typeCounts.forEach((type, count) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", type);
            entry.put("count", count);
            entry.put("percentage", total > 0 ? Math.round((count * 100.0) / total) : 0);
            distribution.add(entry);
        });

        return distribution;
    }

    /**
     * Get upload timeline for chart display (line/bar chart data).
     * Returns uploads grouped by day for the last 30 days.
     */
    public List<Map<String, Object>> getUploadTimeline(UUID userId) {
        List<Document> userDocs = documentRepository.findByUserIdAndStatusNot(
                userId,
                Document.DocumentStatus.DELETED,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        // Group uploads by date (last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        Map<LocalDate, Long> dailyCounts = userDocs.stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .collect(Collectors.groupingBy(
                        d -> d.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", date.toString());
            entry.put("uploads", dailyCounts.getOrDefault(date, 0L));
            timeline.add(entry);
        }

        return timeline;
    }

    /**
     * Get recent user activity (last 10 document uploads).
     */
    public List<Map<String, Object>> getRecentActivity(UUID userId) {
        List<Document> userDocs = documentRepository.findByUserIdAndStatusNot(
                        userId,
                        Document.DocumentStatus.DELETED,
                        org.springframework.data.domain.PageRequest.of(0, 10,
                                org.springframework.data.domain.Sort.by("createdAt").descending())
                )
                .getContent();

        List<Map<String, Object>> activities = new ArrayList<>();
        for (Document doc : userDocs) {
            Map<String, Object> activity = new LinkedHashMap<>();
            activity.put("type", "upload");
            activity.put("documentName", doc.getName());
            activity.put("documentType", doc.getType().name());
            activity.put("status", doc.getStatus().name());
            activity.put("timestamp", doc.getCreatedAt());
            activities.add(activity);
        }

        return activities;
    }

    /**
     * Get top-searched documents (most referenced in message sources).
     */
    public List<Map<String, Object>> getTopDocuments(UUID userId) {
        List<Document> userDocs = documentRepository.findByUserIdAndStatusNot(
                userId,
                Document.DocumentStatus.DELETED,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        // Sort by chunk count (proxy for how much content is searchable)
        List<Map<String, Object>> topDocs = userDocs.stream()
                .sorted(Comparator.comparingInt(
                        (Document d) -> d.getChunksCount() != null ? d.getChunksCount() : 0
                ).reversed())
                .limit(5)
                .map(doc -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", doc.getId());
                    entry.put("name", doc.getName());
                    entry.put("type", doc.getType().name());
                    entry.put("chunksCount", doc.getChunksCount());
                    entry.put("uploadDate", doc.getCreatedAt());
                    return entry;
                })
                .collect(Collectors.toList());

        return topDocs;
    }

    /**
     * Get platform-wide statistics (admin only).
     */
    public Map<String, Object> getPlatformAnalytics() {
        log.info("Generating platform-wide analytics");

        long totalUsers = userRepository.count() - 1;
        long totalDocuments = documentRepository.countByStatusNot(Document.DocumentStatus.DELETED);
        long totalConversations = conversationRepository.count();

        // Count active users (users who logged in within the last 24 hours)
        long activeUsersToday = userRepository.findAll().stream()
                .filter(u -> u.getLastLoginAt() != null &&
                        u.getLastLoginAt().isAfter(LocalDateTime.now().minusDays(1)))
                .count();

        // Calculate total storage
        long totalStorage = documentRepository.findByStatusNot(Document.DocumentStatus.DELETED).stream()
                .mapToLong(d -> d.getSize() != null ? d.getSize() : 0)
                .sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalDocuments", totalDocuments);
        stats.put("totalConversations", totalConversations);
        stats.put("activeUsersToday", activeUsersToday);
        stats.put("totalStorageUsed", totalStorage);

        return stats;
    }

    /**
     * Get system usage timeline for admin dashboard.
     * Shows user registrations and document uploads over the last 30 days.
     */
    public Map<String, Object> getUsageTimeline() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        // User registrations timeline
        Map<LocalDate, Long> userRegistrations = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        // Document uploads timeline
        Map<LocalDate, Long> docUploads = documentRepository.findByStatusNot(Document.DocumentStatus.DELETED).stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .collect(Collectors.groupingBy(
                        d -> d.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", date.toString());
            entry.put("userRegistrations", userRegistrations.getOrDefault(date, 0L));
            entry.put("documentUploads", docUploads.getOrDefault(date, 0L));
            timeline.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timeline", timeline);
        return result;
    }
}
