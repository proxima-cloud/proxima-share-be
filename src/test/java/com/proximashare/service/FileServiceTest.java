package com.proximashare.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

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
import org.springframework.web.multipart.MultipartFile;

import com.proximashare.entity.FileMetadata;
import com.proximashare.repository.FileMetadataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService Unit Tests")
class FileServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private String storagePath;
    private MultipartFile validFile;
    private FileMetadata sampleFileMetadata;

    @BeforeEach
    void setUp() {
        // Create a temporary directory for each test
        storagePath = tempDir.toString();

        // Use reflection to set the storage path (since it's injected via @Value)
        try {
            var field = FileService.class.getDeclaredField("storagePath");
            field.setAccessible(true);
            field.set(fileService, storagePath);
        } catch (Exception e) {
            fail("Failed to set storage path: " + e.getMessage());
        }

        // Create a sample valid file
        validFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Sample file content for testing".getBytes()
        );

        // Create sample metadata
        sampleFileMetadata = new FileMetadata(
                "test-uuid-123",
                "test-document.pdf",
                validFile.getSize(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3),
                0
        );
    }

    @Nested
    @DisplayName("File Upload Tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should successfully upload a valid file")
        void shouldUploadValidFile() {
            // Arrange
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);

            // Return the same object that was passed to save()
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
            assertNotNull(result.getUploadDate());
            assertNotNull(result.getExpiryDate());

            // Verify interactions
            verify(fileMetadataRepository).save(any(FileMetadata.class));
            verify(fileMetadataRepository, atLeastOnce()).existsById(anyString());
        }

        @Test
        @DisplayName("Should reject file larger than 1GB")
        void shouldRejectLargeFile() {
            // Arrange
            MultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large-file.zip",
                    "application/zip",
                    new byte[0] // We'll mock the size
            ) {
                @Override
                public long getSize() {
                    return 1_073_741_825L; // 1GB + 1 byte
                }
            };

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> fileService.uploadFile(largeFile)
            );

            assertEquals("File size exceeds 1GB", exception.getMessage());

            // Verify no database interaction occurred
            verify(fileMetadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle UUID collision by generating new UUID")
        void shouldHandleUuidCollision() {
            // Arrange
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
            MultipartFile fileWithoutExtension = new MockMultipartFile(
                    "file",
                    "README", // No extension
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
            MultipartFile blankNameFile = new MockMultipartFile(
                    "file",
                    filename.trim().isEmpty() ? null : filename,
                    "text/plain",
                    "content".getBytes()
            );
            when(fileMetadataRepository.existsById(anyString())).thenReturn(false);

            // Capture what gets saved to verify the filename
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenAnswer(invocation -> {
                FileMetadata savedMetadata = invocation.getArgument(0);
                return savedMetadata; // Return the same object that was passed in
            });

            // Act
            FileMetadata result = fileService.uploadFile(blankNameFile);

            // Assert
            assertNotNull(result);
            assertEquals("unknown", result.getFilename(),
                    "Blank or null filename should default to 'unknown'");

            // Verify that save was called with correct data
            verify(fileMetadataRepository).save(argThat(metadata ->
                    metadata.getFilename().equals("unknown")
            ));
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
                    uuid,
                    "test.pdf",
                    1000L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1), // Expires tomorrow
                    0
            );
            when(fileMetadataRepository.findById(uuid)).thenReturn(Optional.of(metadata));

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
            when(fileMetadataRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

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
                    expiredUuid,
                    "expired.pdf",
                    1000L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1), // Expired yesterday
                    0
            );
            when(fileMetadataRepository.findById(expiredUuid)).thenReturn(Optional.of(expiredMetadata));

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
        @DisplayName("Should successfully download file and increment count")
        void shouldDownloadFileAndIncrementCount() throws Exception {
            // Arrange
            String uuid = "download-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid,
                    "download-test.txt",
                    1000L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    0
            );

            // Create actual file in temp directory
            File expectedFile = new File(tempDir.toFile(), uuid + ".txt");
            Files.write(expectedFile.toPath(), "test content".getBytes());

            when(fileMetadataRepository.findById(uuid)).thenReturn(Optional.of(metadata));
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(metadata);

            // Act
            File result = fileService.downloadFile(uuid);

            // Assert
            assertNotNull(result);
            assertEquals(expectedFile.getAbsolutePath(), result.getAbsolutePath());

            // Verify download count was incremented and saved
            verify(fileMetadataRepository).save(argThat(savedMetadata ->
                    savedMetadata.getDownloadCount() == 1
            ));
        }

        @Test
        @DisplayName("Should reject download when limit exceeded")
        void shouldRejectDownloadWhenLimitExceeded() {
            // Arrange
            String uuid = "limit-exceeded-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid,
                    "limited-file.txt",
                    1000L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    4 // Already at max downloads (> 3)
            );
            when(fileMetadataRepository.findById(uuid)).thenReturn(Optional.of(metadata));

            // Act & Assert
            IllegalAccessException exception = assertThrows(
                    IllegalAccessException.class,
                    () -> fileService.downloadFile(uuid)
            );

            assertEquals("File download limit reached for this file. (Max. 3 Times)", exception.getMessage());

            // Verify no save operation occurred
            verify(fileMetadataRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle file download at exactly limit (3 downloads)")
        void shouldAllowDownloadAtExactLimit() throws Exception {
            // Arrange
            String uuid = "at-limit-uuid";
            FileMetadata metadata = new FileMetadata(
                    uuid,
                    "at-limit.txt",
                    1000L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1),
                    3 // At limit, should still allow download
            );

            File expectedFile = new File(tempDir.toFile(), uuid + ".txt");
            Files.write(expectedFile.toPath(), "content".getBytes());

            when(fileMetadataRepository.findById(uuid)).thenReturn(Optional.of(metadata));
            when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(metadata);

            // Act
            File result = fileService.downloadFile(uuid);

            // Assert
            assertNotNull(result);
            verify(fileMetadataRepository).save(argThat(savedMetadata ->
                    savedMetadata.getDownloadCount() == 4
            ));
        }

        @Test
        @DisplayName("Should propagate FileNotFoundException from getFileMetadata")
        void shouldPropagateFileNotFoundException() {
            // Arrange
            String uuid = "non-existent-uuid";
            when(fileMetadataRepository.findById(uuid)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    FileNotFoundException.class,
                    () -> fileService.downloadFile(uuid)
            );
        }
    }
}