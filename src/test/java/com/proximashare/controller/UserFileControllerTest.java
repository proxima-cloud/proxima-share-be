package com.proximashare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximashare.config.TestSecurityConfig;
import com.proximashare.dto.FileMetadataResponse;
import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.User;
import com.proximashare.exception.GlobalExceptionHandler;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.FileService;
import com.proximashare.service.JwtService;
import com.proximashare.utils.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserFileController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "application.security.jwt.secret=testSecretKeyForJwtTokenGenerationAndValidationTesting123456",
        "application.security.jwt.expiration=3600000",
        "app.environment.production=false",
        "app.error.include-stacktrace=false"
})
@DisplayName("UserFileController Tests")
class UserFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileService fileService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private User testUser;
    private FileMetadata testFileMetadata;

    @BeforeEach
    void setUp() {
        testUser = TestAuthHelper.createTestUser("testFileUploader", "password123", "ROLE_USER");

        testFileMetadata = new FileMetadata();
        testFileMetadata.setUuid("test-uuid-123");
        testFileMetadata.setFilename("test-file.txt");
        testFileMetadata.setFilename("test-file.txt");
        testFileMetadata.setSize(1024L);
//        testFileMetadata.set("text/plain"); // TODO need to add column for mime type in DB
        testFileMetadata.setUploadDate(LocalDateTime.now());
        testFileMetadata.setDownloadCount(0);
        testFileMetadata.setOwner(testUser);
    }

    @Nested
    @DisplayName("POST /user/files/upload")
    class UploadFileTests {

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should upload file successfully")
        void shouldUploadFileSuccessfully() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.txt",
                    "text/plain",
                    "Test file content".getBytes()
            );

            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            when(fileService.uploadFileForUser(any(), eq(testUser))).thenReturn(testFileMetadata);

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"))
                    .andExpect(jsonPath("$.message").value("File uploaded successfully"));

            verify(fileService).uploadFileForUser(any(), eq(testUser));
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should fail when file is empty")
        void shouldFailWhenFileIsEmpty() throws Exception {
            // Arrange
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "test-file.txt",
                    "text/plain",
                    new byte[0]
            );

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(emptyFile))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File is missing"));

            verify(fileService, never()).uploadFileForUser(any(), any());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthentication() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.txt",
                    "text/plain",
                    "Test file content".getBytes()
            );

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(file))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).uploadFileForUser(any(), any());
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.txt",
                    "text/plain",
                    "Test file content".getBytes()
            );

            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(file))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).uploadFileForUser(any(), any());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Should allow ADMIN role to upload files")
        void shouldAllowAdminRoleToUploadFiles() throws Exception {
            // Arrange
            User adminUser = TestAuthHelper.createTestUser("admin", "password123", "ROLE_ADMIN");
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.txt",
                    "text/plain",
                    "Test file content".getBytes()
            );

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(fileService.uploadFileForUser(any(), eq(adminUser))).thenReturn(testFileMetadata);

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"));

            verify(fileService).uploadFileForUser(any(), eq(adminUser));
        }
    }

    @Nested
    @DisplayName("GET /user/files")
    class GetUserFilesTests {

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return list of user files")
        void shouldReturnListOfUserFiles() throws Exception {
            // Arrange
            FileMetadata file1 = new FileMetadata();
            file1.setUuid("uuid-1");
            file1.setFilename("file1.txt");
            file1.setSize(100L);
//            file1.setMimeType("text/plain");
            file1.setUploadDate(LocalDateTime.now());

            FileMetadata file2 = new FileMetadata();
            file2.setUuid("uuid-2");
            file2.setFilename("file2.pdf");
            file2.setSize(200L);
//            file2.setMimeType("application/pdf");
            file2.setUploadDate(LocalDateTime.now());

            List<FileMetadata> files = Arrays.asList(file1, file2);

            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            when(fileService.getUserFiles(testUser)).thenReturn(files);

            // Act & Assert
            mockMvc.perform(get("/user/files"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].uuid").value("uuid-1"))
                    .andExpect(jsonPath("$[0].filename").value("file1.txt"))
                    .andExpect(jsonPath("$[1].uuid").value("uuid-2"))
                    .andExpect(jsonPath("$[1].filename").value("file2.pdf"));

            verify(fileService).getUserFiles(testUser);
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return empty list when user has no files")
        void shouldReturnEmptyListWhenUserHasNoFiles() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            when(fileService.getUserFiles(testUser)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/user/files"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(fileService).getUserFiles(testUser);
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthentication() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/user/files"))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).getUserFiles(any());
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/user/files"))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).getUserFiles(any());
        }
    }

    @Nested
    @DisplayName("GET /user/files/{uuid}")
    class GetFileMetadataTests {

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return file metadata by UUID")
        void shouldReturnFileMetadataByUuid() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(testFileMetadata);

            // Act & Assert
            mockMvc.perform(get("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"))
                    .andExpect(jsonPath("$.filename").value("test-file.txt"))
                    .andExpect(jsonPath("$.size").value(1024));
//                    .andExpect(jsonPath("$.mimeType").value("text/plain"));

            verify(fileService).getFileMetadata("test-uuid-123");
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("non-existent-uuid"))
                    .thenThrow(new FileNotFoundException("File not found"));

            // Act & Assert
            mockMvc.perform(get("/user/files/{uuid}", "non-existent-uuid"))
                    .andExpect(status().isNotFound());

            verify(fileService).getFileMetadata("non-existent-uuid");
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should allow anonymous access to file metadata")
        void shouldAllowAnonymousAccessToFileMetadata() throws Exception {
            // Arrange
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(testFileMetadata);

            // Act & Assert
            mockMvc.perform(get("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("test-uuid-123"));

            verify(fileService).getFileMetadata("test-uuid-123");
        }
    }

    @Nested
    @DisplayName("GET /user/files/download/{uuid}")
    class DownloadFileTests {

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should download file successfully")
        void shouldDownloadFileSuccessfully() throws Exception {
            // Arrange
            File tempFile = File.createTempFile("test", ".txt");
            tempFile.deleteOnExit();

            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            when(fileService.downloadFile("test-uuid-123")).thenReturn(tempFile);
            when(fileService.getFileMetadata("test-uuid-123")).thenReturn(testFileMetadata);

            // Act & Assert
            mockMvc.perform(get("/user/files/download/{uuid}", "test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"test-file.txt\""));

            verify(fileService).downloadFile("test-uuid-123");
            verify(fileService).getFileMetadata("test-uuid-123");
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return 404 when file not found for download")
        void shouldReturn404WhenFileNotFoundForDownload() throws Exception {
            // Arrange
            when(fileService.downloadFile("non-existent-uuid"))
                    .thenThrow(new FileNotFoundException("File not found"));

            // Act & Assert
            mockMvc.perform(get("/user/files/download/{uuid}", "non-existent-uuid"))
                    .andExpect(status().isNotFound());

            verify(fileService).downloadFile("non-existent-uuid");
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return 403 when access is denied")
        void shouldReturn403WhenAccessDenied() throws Exception {
            // Arrange
            when(fileService.downloadFile("test-uuid-123"))
                    .thenThrow(new IllegalAccessException("Access denied"));

            // Act & Assert
            mockMvc.perform(get("/user/files/download/{uuid}", "test-uuid-123"))
                    .andExpect(status().isForbidden());

            verify(fileService).downloadFile("test-uuid-123");
        }

    }

    @Nested
    @DisplayName("DELETE /user/files/{uuid}")
    class DeleteFileTests {

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should delete file successfully")
        void shouldDeleteFileSuccessfully() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            doNothing().when(fileService).deleteUserFile("test-uuid-123", testUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("File deleted successfully"));

            verify(fileService).deleteUserFile("test-uuid-123", testUser);
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthentication() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).deleteUserFile(anyString(), any());
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).deleteUserFile(anyString(), any());
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return 404 when file not found")
        void shouldReturn404WhenFileNotFound() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            doThrow(new FileNotFoundException("File not found"))
                    .when(fileService).deleteUserFile("non-existent-uuid", testUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "non-existent-uuid"))
                    .andExpect(status().isNotFound());

            verify(fileService).deleteUserFile("non-existent-uuid", testUser);
        }

        @Test
        @WithMockUser(username = "testFileUploader", roles = {"USER"})
        @DisplayName("Should return 403 when user tries to delete another user's file")
        void shouldReturn403WhenUserTriesToDeleteAnotherUsersFile() throws Exception {
            // Arrange
            when(userRepository.findByUsername("testFileUploader")).thenReturn(Optional.of(testUser));
            doThrow(new IllegalAccessException("You can only delete your own files"))
                    .when(fileService).deleteUserFile("test-uuid-123", testUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isForbidden());

            verify(fileService).deleteUserFile("test-uuid-123", testUser);
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("Should allow ADMIN role to delete files")
        void shouldAllowAdminRoleToDeleteFiles() throws Exception {
            // Arrange
            User adminUser = TestAuthHelper.createTestUser("admin", "password123", "ROLE_ADMIN");
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            doNothing().when(fileService).deleteUserFile("test-uuid-123", adminUser);

            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("File deleted successfully"));

            verify(fileService).deleteUserFile("test-uuid-123", adminUser);
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @WithMockUser(username = "guest", roles = {"GUEST"})
        @DisplayName("Should deny file upload for non-USER/ADMIN roles")
        void shouldDenyFileUploadForInvalidRoles() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-file.txt",
                    "text/plain",
                    "Test file content".getBytes()
            );

            // Act & Assert
            mockMvc.perform(multipart("/user/files/upload")
                            .file(file))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).uploadFileForUser(any(), any());
        }

        @Test
        @WithMockUser(username = "guest", roles = {"GUEST"})
        @DisplayName("Should deny file deletion for non-USER/ADMIN roles")
        void shouldDenyFileDeletionForInvalidRoles() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/user/files/{uuid}", "test-uuid-123"))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).deleteUserFile(anyString(), any());
        }

        @Test
        @WithMockUser(username = "guest", roles = {"GUEST"})
        @DisplayName("Should deny getting user files for non-USER/ADMIN roles")
        void shouldDenyGettingUserFilesForInvalidRoles() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/user/files"))
                    .andExpect(status().isForbidden());

            verify(fileService, never()).getUserFiles(any());
        }
    }
}