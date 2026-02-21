package com.cognify.cognify_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceChunkDto {
    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private Integer pageNumber;
    private String snippet; // First 200 chars of chunk
    private Float relevanceScore;
}
