package com.cognify.cognify_backend.controller;

import com.cognify.cognify_backend.exception.FileStorageException;
import com.cognify.cognify_backend.exception.GlobalExceptionHandler;
import com.cognify.cognify_backend.dto.DocumentDto;
import com.cognify.cognify_backend.security.JwtAuthenticationFilter;
import com.cognify.cognify_backend.security.UserPrincipal;
import com.cognify.cognify_backend.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    void uploadUnsupportedFileTypeReturns400() throws Exception {
        when(documentService.uploadDocument(any(), any()))
                .thenThrow(new FileStorageException("File type not allowed"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "dummy".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FILE_VALIDATION_ERROR"));
    }

            @Test
            @WithMockUser
            void getUserDocumentsReturnsStablePaginationEnvelope() throws Exception {
            DocumentDto document = DocumentDto.builder()
                .id(UUID.randomUUID())
                .name("notes.pdf")
                .type("pdf")
                .size(1024L)
                .status("indexed")
                .createdAt(LocalDateTime.now())
                .build();

            when(documentService.getUserDocuments(any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of(document)));

            mockMvc.perform(get("/api/documents")
                    .param("page", "0")
                    .param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("notes.pdf"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.number").value(0));
            }
}
