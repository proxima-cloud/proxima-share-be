package com.proximashare.controller;

import com.proximashare.dto.FileMetadataResponse;
import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.User;
import com.proximashare.exception.GlobalExceptionHandler;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.FileService;
import com.proximashare.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserFileController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@ContextConfiguration(classes = {UserFileController.class, GlobalExceptionHandler.class, SecurityTestConfig.class})
@TestPropertySource(properties = {
        "app.environment.production=false",
        "app.error.include-stacktrace=false"
})
@DisplayName("UserFileController Web Layer Tests")
class UserFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private UserRepository userRepository;

    @TempDir
    Path tempDir;

    private User testUser;
    private FileMetadata sampleMetadata;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setAuthProvider("LOCAL");
        testUser.setRoles(new HashSet<>());

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
    @DisplayName("POST /user/files/upload")
    class UploadFileTests {

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should successfully upload a file for authenticated user")
        void shouldUploadFileForAuthenticatedUser() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(fileService.uploadFileForUser(any(), eq(testUser))).thenReturn(sampleMetadata);

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(validFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"))
                    .andExpect(jsonPath("$.message").value("File uploaded successfully"));

            verify(userRepository).findByUsername("testuser");
            verify(fileService).uploadFileForUser(any(), eq(testUser));
        }

        @Test
        @WithMockUser(username = "testuser")
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
            mockMvc.perform(multipart("/user/files/upload")
                            .file(emptyFile)
                            .with(user("testuser")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File is missing"));

            verify(fileService, never()).uploadFileForUser(any(), any());
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(validFile))
                    .andExpect(status().isUnauthorized());

            verify(fileService, never()).uploadFileForUser(any(), any());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should handle IllegalArgumentException from service")
        void shouldHandleIllegalArgumentException() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(fileService.uploadFileForUser(any(), eq(testUser)))
                    .thenThrow(new IllegalArgumentException("File size exceeds limit"));

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(validFile)
                            .with(user("testuser")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File size exceeds limit"));
        }
    }

    @Nested
    @DisplayName("GET /user/files")
    class GetUserFilesTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return list of user files")
        void shouldReturnUserFiles() throws Exception {
            // Arrange
            FileMetadata file1 = new FileMetadata("uuid1", "file1.pdf", 1024L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(3), 0);
            FileMetadata file2 = new FileMetadata("uuid2", "file2.pdf", 2048L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(3), 1);

            List<FileMetadata> files = Arrays.asList(file1, file2);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(fileService.getUserFiles(testUser)).thenReturn(files);

            // Act & Assert
            mockMvc.perform(get("/user/files")
                            .with(user("testuser")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].uuid").value("uuid1"))
                    .andExpect(jsonPath("$[1].uuid").value("uuid2"));

            verify(userRepository).findByUsername("testuser");
            verify(fileService).getUserFiles(testUser);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return empty list when user has no files")
        void shouldReturnEmptyListWhenNoFiles() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(fileService.getUserFiles(testUser)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/user/files")
                            .with(user("testuser")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/user/files"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /user/files/{uuid}")
    class GetFileMetadataTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return file metadata for valid UUID")
        void shouldReturnMetadataForValidUuid() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(sampleMetadata);

            // Act & Assert
            mockMvc.perform(get("/user/files/test-uuid-123")
                            .with(user("testuser")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"))
                    .andExpect(jsonPath("$.filename").value("test-document.pdf"))
                    .andExpect(jsonPath("$.size").value(1024));

            verify(fileService).getFileMetadata("test-uuid-123");
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("non-existent"))
                    .thenThrow(new FileNotFoundException("File not found"));

            // Act & Assert
            mockMvc.perform(get("/user/files/non-existent")
                            .with(user("testuser")))
                    .andExpect(status().isNotFound());
        }
/* THIS CASE IS WHEN THE FILE IS PROTECTED AND ONLY SHARED WITH SPECIFIC USERS
                SHOULD BE PREMIUM MEMBERS FEATURE - PROTECTED FILES
* */
//        @Test
//        @DisplayName("Should return 401 when user is not authenticated")
//        void shouldReturn401WhenNotAuthenticated() throws Exception {
//            // Arrange
//            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(sampleMetadata);
//            // Act & Assert
//            mockMvc.perform(get("/user/files/test-uuid-123"))
//                    .andExpect(status().isUnauthorized());
//        }
    }

    @Nested
    @DisplayName("GET /user/files/download/{uuid}")
    class DownloadFileTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should successfully download a file")
        void shouldDownloadFile() throws Exception {
            // Arrange
            File testFile = tempDir.resolve("test-uuid-123.pdf").toFile();
            Files.write(testFile.toPath(), "Test file content".getBytes());

            when(fileService.downloadFile("test-uuid-123")).thenReturn(testFile);
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(sampleMetadata);

            // Act & Assert
            mockMvc.perform(get("/user/files/download/test-uuid-123")
                            .with(user("testuser")))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"test-document.pdf\""))
                    .andExpect(content().bytes("Test file content".getBytes()));

            verify(fileService).downloadFile("test-uuid-123");
            verify(fileService).getFileMetadata("test-uuid-123");
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Arrange
            when(fileService.downloadFile("non-existent"))
                    .thenThrow(new FileNotFoundException("File not found"));

            // Act & Assert
            mockMvc.perform(get("/user/files/download/non-existent")
                            .with(user("testuser")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return 403 when download limit exceeded")
        void shouldReturn403WhenDownloadLimitExceeded() throws Exception {
            // Arrange
            when(fileService.downloadFile("limited-uuid"))
                    .thenThrow(new IllegalAccessException("Download limit reached"));

            // Act & Assert
            mockMvc.perform(get("/user/files/download/limited-uuid")
                            .with(user("testuser")))
                    .andExpect(status().isForbidden());
        }
/* THIS CASE IS WHEN THE FILE IS PROTECTED AND ONLY SHARED WITH SPECIFIC USERS
                SHOULD BE PREMIUM MEMBERS FEATURE - PROTECTED FILES
* */
//        @Test
//        @DisplayName("Should return 401 when user is not authenticated")
//        void shouldReturn401WhenNotAuthenticated() throws Exception {
//            // Arrange
//            File testFile = tempDir.resolve("test-uuid-123.pdf").toFile();
//            Files.write(testFile.toPath(), "Test file content".getBytes());
//
//            when(fileService.downloadFile("test-uuid-123")).thenReturn(testFile);
//            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(sampleMetadata);
//            // Act & Assert
//            mockMvc.perform(get("/user/files/download/test-uuid-123"))
//                    .andExpect(status().isUnauthorized());
//        }
    }

    @Nested
    @DisplayName("DELETE /user/files/{uuid}")
    class DeleteFileTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should successfully delete user file")
        void shouldDeleteUserFile() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            doNothing().when(fileService).deleteUserFile("test-uuid-123", testUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/test-uuid-123")
                            .with(user("testuser")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("File deleted successfully"));

            verify(userRepository).findByUsername("testuser");
            verify(fileService).deleteUserFile("test-uuid-123", testUser);
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            doThrow(new FileNotFoundException("File not found"))
                    .when(fileService).deleteUserFile("non-existent", testUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/non-existent")
                            .with(user("testuser")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/user/files/test-uuid-123"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("Should handle unauthorized deletion attempt")
        void shouldHandleUnauthorizedDeletion() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            doThrow(new IllegalAccessException("Not authorized to delete this file"))
                    .when(fileService).deleteUserFile("other-user-file", testUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/other-user-file")
                            .with(user("testuser")))
                    .andExpect(status().isForbidden());
        }
    }
}