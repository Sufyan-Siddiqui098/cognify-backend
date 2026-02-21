package com.cognify.cognify_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private UUID id;
    private UUID conversationId;
    private String sender; // USER or ASSISTANT
    private String content;
    private List<SourceChunkDto> sources; // For ASSISTANT messages
    private LocalDateTime createdAt;
}
