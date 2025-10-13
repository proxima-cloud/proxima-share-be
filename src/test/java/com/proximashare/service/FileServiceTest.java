package com.proximashare.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.proximashare.app.config.FileUploadConfig;
import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.FileMetadataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests")
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private FileUploadConfig uploadConfig;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private String storagePath;
    private MultipartFile validFile;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a temporary directory for each test
        storagePath = tempDir.toString();

        // Use ReflectionTestUtils to set the storage path
        ReflectionTestUtils.setField(fileService, "storagePath", storagePath);

        // Create a sample valid file
        validFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Sample file content for testing".getBytes()
        );

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRoles(Set.of(new Role(2L, "USER")));
    }

    @Nested
    @DisplayName("Public File Upload Tests")
    class PublicUploadFileTests {

        @Test
        @DisplayName("Should successfully upload a valid public file")
        void shouldUploadValidPublicFile() {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);
            when(uploadConfig.getPublicExpiryDays()).thenReturn(7);
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFile(validFile);

            // Assert
            assertNotNull(result);
            assertEquals("test-document.pdf", result.getFilename());
            assertEquals(validFile.getSize(), result.getSize());
            assertEquals(0, result.getDownloadCount());
            assertTrue(result.isPublic(), "File should be marked as public");
            assertNull(result.getOwner(), "Public file should have no owner");
            assertNotNull(result.getUploadDate());
            assertNotNull(result.getExpiryDate());

            // Verify expiry is 7 days (public default)
            assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now().plusDays(6)));
            assertTrue(result.getExpiryDate().isBefore(LocalDateTime.now().plusDays(8)));

            verify(fileMetadataRepository).save(any(FileMetadata.class));
            verify(fileMetadataRepository, atLeastOnce()).existsById(anyString());
        }

        @Test
        @DisplayName("Should reject public file larger than 1GB")
        void shouldRejectLargePublicFile() {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);

            MultipartFile largeFile = mock(MultipartFile.class);
            when(largeFile.getSize()).thenReturn(1_073_741_825L); // 1GB + 1 byte

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> fileService.uploadFile(largeFile)
            );

            assertTrue(exception.getMessage().contains("File size exceeds"));
            assertTrue(exception.getMessage().contains("GB limit"));

            verify(fileMetadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle UUID collision by generating new UUID")
        void shouldHandleUuidCollision() {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);
            when(uploadConfig.getPublicExpiryDays()).thenReturn(7);
            when(fileMetadataRepository.existsById(anyString()))
                    .thenReturn(true)   // First UUID exists
                    .thenReturn(false); // Second UUID doesn't exist
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFile(validFile);

            // Assert
            assertNotNull(result);
            verify(fileMetadataRepository, times(2)).existsById(anyString());
            verify(fileMetadataRepository).save(any(FileMetadata.class));
        }

        @Test
        @DisplayName("Should handle file with no extension")
        void shouldHandleFileWithoutExtension() {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);
            when(uploadConfig.getPublicExpiryDays()).thenReturn(7);
            MultipartFile fileWithoutExtension = new MockMultipartFile(
                    "file",
                    "README",
                    "text/plain",
                    "Sample content".getBytes()
            );
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFile(fileWithoutExtension);

            // Assert
            assertNotNull(result);
            assertEquals("README", result.getFilename());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should handle files with blank/null names")
        void shouldHandleBlankFilename(String filename) {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);
            when(uploadConfig.getPublicExpiryDays()).thenReturn(7);
            MultipartFile blankNameFile = new MockMultipartFile(
                    "file",
                    filename.trim().isEmpty() ? null : filename,
                    "text/plain",
                    "content".getBytes()
            );
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFile(blankNameFile);

            // Assert
            assertNotNull(result);
            assertEquals("unknown", result.getFilename());
            verify(fileMetadataRepository).save(argThat(metadata ->
                    metadata.getFilename().equals("unknown")
            ));
        }
    }

    @Nested
    @DisplayName("User File Upload Tests")
    class UserUploadFileTests {

        @Test
        @DisplayName("Should successfully upload file for authenticated user")
        void shouldUploadFileForUser() {
            // Arrange
            when(uploadConfig.getUserMaxSize()).thenReturn(5_368_709_120L);
            when(uploadConfig.getUserExpiryDays()).thenReturn(30);
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFileForUser(validFile, testUser);

            // Assert
            assertNotNull(result);
            assertEquals("test-document.pdf", result.getFilename());
            assertEquals(validFile.getSize(), result.getSize());
            assertEquals(0, result.getDownloadCount());
            assertFalse(result.isPublic(), "User file should not be public");
            assertEquals(testUser, result.getOwner(), "File should be owned by user");
            assertNotNull(result.getUploadDate());
            assertNotNull(result.getExpiryDate());

            // Verify expiry is 30 days (user default)
            assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now().plusDays(29)));
            assertTrue(result.getExpiryDate().isBefore(LocalDateTime.now().plusDays(31)));

            verify(fileMetadataRepository).save(any(FileMetadata.class));
        }

        @Test
        @DisplayName("Should reject user file larger than 5GB")
        void shouldRejectLargeUserFile() {
            // Arrange
            when(uploadConfig.getUserMaxSize()).thenReturn(5_368_709_120L);

            MultipartFile largeFile = mock(MultipartFile.class);
            when(largeFile.getSize()).thenReturn(5_368_709_121L); // 5GB + 1 byte

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> fileService.uploadFileForUser(largeFile, testUser)
            );

            assertTrue(exception.getMessage().contains("File size exceeds"));
            verify(fileMetadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow user to upload file up to 5GB (larger than public limit)")
        void shouldAllowUserUploadLargerThanPublicLimit() {
            // Arrange - File between 1GB and 5GB
            when(uploadConfig.getUserMaxSize()).thenReturn(5_368_709_120L);
            when(uploadConfig.getUserExpiryDays()).thenReturn(30);

            MultipartFile largeFile = mock(MultipartFile.class);
            when(largeFile.getSize()).thenReturn(2_147_483_648L); // 2GB
            when(largeFile.getOriginalFilename()).thenReturn("large-user-file.zip");
//            when(largeFile.isEmpty()).thenReturn(false);

            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFileForUser(largeFile, testUser);

            // Assert
            assertNotNull(result);
            assertEquals(2_147_483_648L, result.getSize());
            assertEquals(testUser, result.getOwner());
            verify(fileMetadataRepository).save(any(FileMetadata.class));
        }
    }

    @Nested
    @DisplayName("User File Management Tests")
    class UserFileManagementTests {

        @Test
        @DisplayName("Should retrieve all files for a user")
        void shouldGetUserFiles() {
            // Arrange
            FileMetadata file1 = new FileMetadata(
                    "uuid1", "file1.pdf", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    0, testUser, false
            );
            FileMetadata file2 = new FileMetadata(
                    "uuid2", "file2.txt", 2000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    0, testUser, false
            );
            List<FileMetadata> userFiles = Arrays.asList(file1, file2);
            when(fileMetadataRepository.findByOwnerOrderByUploadDateDesc(testUser))
                    .thenReturn(userFiles);

            // Act
            List<FileMetadata> result = fileService.getUserFiles(testUser);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("uuid1", result.get(0).getUuid());
            assertEquals("uuid2", result.get(1).getUuid());
            verify(fileMetadataRepository).findByOwnerOrderByUploadDateDesc(testUser);
        }

        @Test
        @DisplayName("Should return empty list when user has no files")
        void shouldReturnEmptyListForUserWithNoFiles() {
            // Arrange
            when(fileMetadataRepository.findByOwnerOrderByUploadDateDesc(testUser))
                    .thenReturn(Arrays.asList());

            // Act
            List<FileMetadata> result = fileService.getUserFiles(testUser);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should delete user's own file successfully")
        void shouldDeleteUserFile() throws Exception {
            // Arrange
            String uuid = "delete-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid, "delete-me.txt", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    0, testUser, false
            );

            // Create actual file in temp directory
            File physicalFile = new File(tempDir.toFile(), uuid + ".txt");
            Files.write(physicalFile.toPath(), "test content".getBytes());

            when(fileMetadataRepository.findByUuidAndOwner(uuid, testUser))
                    .thenReturn(Optional.of(metadata));

            // Act
            fileService.deleteUserFile(uuid, testUser);

            // Assert
            assertFalse(physicalFile.exists(), "Physical file should be deleted");
            verify(fileMetadataRepository).delete(metadata);
        }

        @Test
        @DisplayName("Should throw exception when user tries to delete non-owned file")
        void shouldRejectDeleteOfNonOwnedFile() {
            // Arrange
            String uuid = "not-owned-uuid";
            User otherUser = new User();
            otherUser.setId(2L);
            otherUser.setUsername("otheruser");

            when(fileMetadataRepository.findByUuidAndOwner(uuid, otherUser))
                    .thenReturn(Optional.empty());

            // Act & Assert
            FileNotFoundException exception = assertThrows(
                    FileNotFoundException.class,
                    () -> fileService.deleteUserFile(uuid, otherUser)
            );

            assertEquals("File not found or you don't own this file", exception.getMessage());
            verify(fileMetadataRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle deletion when physical file doesn't exist")
        void shouldHandleDeleteWhenPhysicalFileNotFound() throws Exception {
            // Arrange
            String uuid = "missing-physical-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid, "missing.txt", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    0, testUser, false
            );

            when(fileMetadataRepository.findByUuidAndOwner(uuid, testUser))
                    .thenReturn(Optional.of(metadata));

            // Act - should not throw even if physical file doesn't exist
            assertDoesNotThrow(() -> fileService.deleteUserFile(uuid, testUser));

            // Assert
            verify(fileMetadataRepository).delete(metadata);
        }
    }

    @Nested
    @DisplayName("Get File Metadata Tests")
    class GetFileMetadataTests {

        @Test
        @DisplayName("Should return metadata for valid, non-expired file")
        void shouldReturnMetadataForValidFile() throws FileNotFoundException {
            // Arrange
            String uuid = "valid-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid, "test.pdf", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                    0
            );
            metadata.setPublic(true);
            when(fileMetadataRepository.findByIdWithOwner(uuid))
                    .thenReturn(Optional.of(metadata));

            // Act
            FileMetadata result = fileService.getFileMetadata(uuid);

            // Assert
            assertNotNull(result);
            assertEquals(uuid, result.getUuid());
            assertEquals("test.pdf", result.getFilename());
        }

        @Test
        @DisplayName("Should throw FileNotFoundException for non-existent file")
        void shouldThrowExceptionForNonExistentFile() {
            // Arrange
            String nonExistentUuid = "non-existent";
            when(fileMetadataRepository.findByIdWithOwner(nonExistentUuid))
                    .thenReturn(Optional.empty());

            // Act & Assert
            FileNotFoundException exception = assertThrows(
                    FileNotFoundException.class,
                    () -> fileService.getFileMetadata(nonExistentUuid)
            );

            assertEquals("File not found or expired", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for expired file")
        void shouldThrowExceptionForExpiredFile() {
            // Arrange
            String expiredUuid = "expired-uuid";
            FileMetadata expiredMetadata = new FileMetadata(
                    expiredUuid, "expired.pdf", 1000L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1), // Expired yesterday
                    0
            );
            when(fileMetadataRepository.findByIdWithOwner(expiredUuid))
                    .thenReturn(Optional.of(expiredMetadata));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> fileService.getFileMetadata(expiredUuid)
            );

            assertEquals("File expired", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Download File Tests")
    class DownloadFileTests {

        @Test
        @DisplayName("Should download public file and increment count")
        void shouldDownloadPublicFile() throws Exception {
            // Arrange
            when(uploadConfig.getPublicMaxDownloads()).thenReturn(3);
            String uuid = "public-download-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid, "public-file.txt", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                    0
            );
            metadata.setPublic(true);

            File expectedFile = new File(tempDir.toFile(), uuid + ".txt");
            Files.write(expectedFile.toPath(), "test content".getBytes());

            when(fileMetadataRepository.findByIdWithOwner(uuid))
                    .thenReturn(Optional.of(metadata));
            when(fileMetadataRepository.save(any(FileMetadata.class)))
                    .thenReturn(metadata);

            // Act
            File result = fileService.downloadFile(uuid);

            // Assert
            assertNotNull(result);
            assertEquals(expectedFile.getAbsolutePath(), result.getAbsolutePath());
            verify(fileMetadataRepository).save(argThat(savedMetadata ->
                    savedMetadata.getDownloadCount() == 1
            ));
        }

        @Test
        @DisplayName("Should reject public file download when limit exceeded (>3)")
        void shouldRejectPublicDownloadWhenLimitExceeded() {
            // Arrange
            when(uploadConfig.getPublicMaxDownloads()).thenReturn(3);
            String uuid = "limit-exceeded-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid, "limited-file.txt", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                    3 // At limit
            );
            metadata.setPublic(true);
            when(fileMetadataRepository.findByIdWithOwner(uuid))
                    .thenReturn(Optional.of(metadata));

            // Act & Assert
            IllegalAccessException exception = assertThrows(
                    IllegalAccessException.class,
                    () -> fileService.downloadFile(uuid)
            );

            assertTrue(exception.getMessage().contains("File download limit reached"));
            assertTrue(exception.getMessage().contains("Max. 3 Times"));
            verify(fileMetadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow user file download with higher limit (100)")
        void shouldAllowUserFileDownloadWithHigherLimit() throws Exception {
            // Arrange
            when(uploadConfig.getUserMaxDownloads()).thenReturn(100);
            String uuid = "user-file-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid, "user-file.txt", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    50, testUser, false // 50 downloads, still under 100 limit
            );

            File expectedFile = new File(tempDir.toFile(), uuid + ".txt");
            Files.write(expectedFile.toPath(), "user content".getBytes());

            when(fileMetadataRepository.findByIdWithOwner(uuid))
                    .thenReturn(Optional.of(metadata));
            when(fileMetadataRepository.save(any(FileMetadata.class)))
                    .thenReturn(metadata);

            // Act
            File result = fileService.downloadFile(uuid);

            // Assert
            assertNotNull(result);
            verify(fileMetadataRepository).save(argThat(savedMetadata ->
                    savedMetadata.getDownloadCount() == 51
            ));
        }

        @Test
        @DisplayName("Should reject user file download when limit exceeded (>=100)")
        void shouldRejectUserFileDownloadWhenLimitExceeded() {
            // Arrange
            when(uploadConfig.getUserMaxDownloads()).thenReturn(100);
            String uuid = "user-limit-exceeded";
            FileMetadata metadata = new FileMetadata(
                    uuid, "user-limited.txt", 1000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    100, testUser, false // At limit
            );

            when(fileMetadataRepository.findByIdWithOwner(uuid))
                    .thenReturn(Optional.of(metadata));

            // Act & Assert
            IllegalAccessException exception = assertThrows(
                    IllegalAccessException.class,
                    () -> fileService.downloadFile(uuid)
            );

            assertTrue(exception.getMessage().contains("File download limit reached"));
            assertTrue(exception.getMessage().contains("Max. 100 Times"));
            verify(fileMetadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate FileNotFoundException from getFileMetadata")
        void shouldPropagateFileNotFoundException() {
            // Arrange
            String uuid = "non-existent-uuid";
            when(fileMetadataRepository.findByIdWithOwner(uuid))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    FileNotFoundException.class,
                    () -> fileService.downloadFile(uuid)
            );
        }
    }

    @Nested
    @DisplayName("Helper Method Tests")
    class HelperMethodTests {

        @Test
        @DisplayName("Should extract file extension correctly")
        void shouldExtractFileExtension() throws Exception {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);
            when(uploadConfig.getPublicExpiryDays()).thenReturn(7);
            MultipartFile pdfFile = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "content".getBytes()
            );
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFile(pdfFile);

            // Assert
            assertEquals("document.pdf", result.getFilename());
        }

        @Test
        @DisplayName("Should handle multiple dots in filename")
        void shouldHandleMultipleDotsInFilename() {
            // Arrange
            when(uploadConfig.getPublicMaxSize()).thenReturn(1_073_741_824L);
            when(uploadConfig.getPublicExpiryDays()).thenReturn(7);
            MultipartFile complexFile = new MockMultipartFile(
                    "file", "my.backup.file.tar.gz", "application/gzip", "content".getBytes()
            );
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation ->
                    invocation.getArgument(0)
            );

            // Act
            FileMetadata result = fileService.uploadFile(complexFile);

            // Assert
            assertEquals("my.backup.file.tar.gz", result.getFilename());
        }
    }
}