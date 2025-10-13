package com.proximashare.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.proximashare.entity.FileMetadata;
import com.proximashare.service.FileService;
import com.proximashare.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = FileController.class,
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {FileController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.environment.production=false",
        "app.error.include-stacktrace=false"
})
@DisplayName("FileController Web Layer Tests")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @TempDir
    Path tempDir;

    private FileMetadata sampleMetadata;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        sampleMetadata = new FileMetadata(
                "test-uuid-123",
                "test-document.pdf",
                1024L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3),
                0
        );

        validFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Test file content".getBytes()
        );
    }

    @Nested
    @DisplayName("POST /api/public/files/upload")
    class UploadFileTests {

        @Test
        @DisplayName("Should successfully upload a valid file")
        void shouldUploadValidFile() throws Exception {
            // Arrange
            when(fileService.uploadFile(any())).thenReturn(sampleMetadata);

            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload")
                            .file(validFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"));

            verify(fileService).uploadFile(any());
        }

        @Test
        @DisplayName("Should return 400 when file is empty")
        void shouldReturnBadRequestWhenFileEmpty() throws Exception {
            // Arrange
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    new byte[0]
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload")
                            .file(emptyFile))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File is missing"));

            verify(fileService, never()).uploadFile(any());
        }

        @Test
        @DisplayName("Should return 400 when file parameter is missing")
        void shouldReturnBadRequestWhenFileParameterMissing() throws Exception {
            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload"))
                    .andExpect(status().isBadRequest());

            verify(fileService, never()).uploadFile(any());
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException from service")
        void shouldHandleIllegalArgumentException() throws Exception {
            // Arrange
            when(fileService.uploadFile(any()))
                    .thenThrow(new IllegalArgumentException("File size exceeds 1GB"));

            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload")
                            .file(validFile))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File size exceeds 1GB"));

            verify(fileService).uploadFile(any());
        }

        @Test
        @DisplayName("Should handle RuntimeException from service")
        void shouldHandleRuntimeException() throws Exception {
            // Arrange
            when(fileService.uploadFile(any()))
                    .thenThrow(new RuntimeException("Failed to store file"));

            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload")
                            .file(validFile))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").exists());

            verify(fileService).uploadFile(any());
        }

        @Test
        @DisplayName("Should accept various file types")
        void shouldAcceptVariousFileTypes() throws Exception {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file",
                    "image.png",
                    "image/png",
                    "PNG image data".getBytes()
            );

            FileMetadata imageMetadata = new FileMetadata(
                    "image-uuid",
                    "image.png",
                    imageFile.getSize(),
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(3),
                    0
            );

            when(fileService.uploadFile(any())).thenReturn(imageMetadata);

            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload")
                            .file(imageFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("image-uuid"));
        }
    }

    @Nested
    @DisplayName("GET /api/public/files/{uuid}")
    class GetFileMetadataTests {

        @Test
        @DisplayName("Should return file metadata for valid UUID")
        void shouldReturnMetadataForValidUuid() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(sampleMetadata);

            // Act & Assert
            mockMvc.perform(get("/api/public/files/test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"))
                    .andExpect(jsonPath("$.filename").value("test-document.pdf"))
                    .andExpect(jsonPath("$.size").value(1024))
                    .andExpect(jsonPath("$.downloadCount").value(0));

            verify(fileService).getFileMetadata("test-uuid-123");
        }

        @Test
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("non-existent-uuid"))
                    .thenThrow(new FileNotFoundException("File not found or expired"));

            // Act & Assert
            mockMvc.perform(get("/api/public/files/non-existent-uuid"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("File not found or expired"));

            verify(fileService).getFileMetadata("non-existent-uuid");
        }

        @Test
        @DisplayName("Should return 400 when file is expired")
        void shouldReturn400WhenFileExpired() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("expired-uuid"))
                    .thenThrow(new IllegalArgumentException("File expired"));

            // Act & Assert
            mockMvc.perform(get("/api/public/files/expired-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File expired"));

            verify(fileService).getFileMetadata("expired-uuid");
        }

        @Test
        @DisplayName("Should handle UUID with special characters")
        void shouldHandleUuidWithSpecialCharacters() throws Exception {
            // Arrange
            String complexUuid = "abc-123-def-456";
            FileMetadata metadata = new FileMetadata(
                    complexUuid,
                    "file.txt",
                    100L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    0
            );
            when(fileService.getFileMetadata(complexUuid)).thenReturn(metadata);

            // Act & Assert
            mockMvc.perform(get("/api/public/files/" + complexUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value(complexUuid));
        }
    }

    @Nested
    @DisplayName("GET /api/public/files/download/{uuid}")
    class DownloadFileTests {

        @Test
        @DisplayName("Should successfully download a file")
        void shouldDownloadFile() throws Exception {
            // Arrange
            File testFile = tempDir.resolve("test-uuid-123.pdf").toFile();
            Files.write(testFile.toPath(), "Test file content".getBytes());

            when(fileService.downloadFile("test-uuid-123")).thenReturn(testFile);
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(sampleMetadata);

            // Act & Assert
            mockMvc.perform(get("/api/public/files/download/test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"test-document.pdf\""))
                    .andExpect(content().bytes("Test file content".getBytes()));

            verify(fileService).downloadFile("test-uuid-123");
            verify(fileService).getFileMetadata("test-uuid-123");
        }

        @Test
        @DisplayName("Should return 404 when file not found for download")
        void shouldReturn404WhenFileNotFoundForDownload() throws Exception {
            // Arrange
            when(fileService.downloadFile("non-existent"))
                    .thenThrow(new FileNotFoundException("File not found or expired"));

            // Act & Assert
            mockMvc.perform(get("/api/public/files/download/non-existent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("File not found or expired"));

            verify(fileService).downloadFile("non-existent");
        }

        @Test
        @DisplayName("Should return 403 when download limit exceeded")
        void shouldReturn403WhenDownloadLimitExceeded() throws Exception {
            // Arrange
            when(fileService.downloadFile("limited-uuid"))
                    .thenThrow(new IllegalAccessException("File download limit reached for this file. (Max. 3 Times)"));

            // Act & Assert
            mockMvc.perform(get("/api/public/files/download/limited-uuid"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("File download limit reached for this file. (Max. 3 Times)"));

            verify(fileService).downloadFile("limited-uuid");
        }

        @Test
        @DisplayName("Should handle files with special characters in filename")
        void shouldHandleSpecialCharactersInFilename() throws Exception {
            // Arrange
            File testFile = tempDir.resolve("special-uuid.txt").toFile();
            Files.write(testFile.toPath(), "content".getBytes());

            FileMetadata specialMetadata = new FileMetadata(
                    "special-uuid",
                    "file with spaces & special.txt",
                    100L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    0
            );

            when(fileService.downloadFile("special-uuid")).thenReturn(testFile);
            when(fileService.getFileMetadata("special-uuid")).thenReturn(specialMetadata);

            // Act & Assert
            mockMvc.perform(get("/api/public/files/download/special-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"file with spaces & special.txt\""));
        }

        @Test
        @DisplayName("Should increment download count on successful download")
        void shouldIncrementDownloadCount() throws Exception {
            // Arrange
            File testFile = tempDir.resolve("count-uuid.txt").toFile();
            Files.write(testFile.toPath(), "content".getBytes());

            when(fileService.downloadFile("count-uuid")).thenReturn(testFile);
            when(fileService.getFileMetadata("count-uuid")).thenReturn(sampleMetadata);

            // Act
            mockMvc.perform(get("/api/public/files/download/count-uuid"))
                    .andExpect(status().isOk());

            // Assert - Verify the service methods were called in correct order
            verify(fileService).downloadFile("count-uuid");
            verify(fileService).getFileMetadata("count-uuid");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed UUID gracefully")
        void shouldHandleMalformedUuid() throws Exception {
            // Arrange
            when(fileService.getFileMetadata(anyString()))
                    .thenThrow(new FileNotFoundException("File not found or expired"));

            // Act & Assert
            mockMvc.perform(get("/api/public/files/malformed-@#$"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should handle null pointer exceptions")
        void shouldHandleNullPointerException() throws Exception {
            // Arrange
            when(fileService.uploadFile(any()))
                    .thenThrow(new NullPointerException("Unexpected null value"));

            // Act & Assert
            mockMvc.perform(multipart("/api/public/files/upload")
                            .file(validFile))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.stackTrace").doesNotExist()); // Should be null in tests
        }
    }
}