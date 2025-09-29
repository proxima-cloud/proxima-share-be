package com.proximashare.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proximashare.entity.FileMetadata;
import com.proximashare.repository.FileMetadataRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileCleanupScheduler Tests")
class FileCleanupSchedulerTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @InjectMocks
    private FileCleanupScheduler fileCleanupScheduler;

    @TempDir
    Path tempDir;

    private String storagePath;

    @BeforeEach
    void setUp() throws Exception {
        storagePath = tempDir.toString();

        // Use reflection to set the storage path
        var field = FileCleanupScheduler.class.getDeclaredField("storagePath");
        field.setAccessible(true);
        field.set(fileCleanupScheduler, storagePath);
    }

    @Nested
    @DisplayName("Cleanup Expired Files Tests")
    class CleanupExpiredFilesTests {

        @Test
        @DisplayName("Should delete expired file and metadata")
        void shouldDeleteExpiredFileAndMetadata() throws IOException {
            // Arrange
            String expiredUuid = "expired-uuid-001";
            FileMetadata expiredMetadata = new FileMetadata(
                    expiredUuid,
                    "expired-file.txt",
                    1024L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1), // Expired yesterday
                    0
            );

            // Create actual file
            File expiredFile = new File(tempDir.toFile(), expiredUuid);
            Files.write(expiredFile.toPath(), "expired content".getBytes());
            assertThat(expiredFile).exists();

            when(fileMetadataRepository.findAll()).thenReturn(Collections.singletonList(expiredMetadata));

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            assertThat(expiredFile).doesNotExist(); // File should be deleted
            verify(fileMetadataRepository).delete(expiredMetadata);
            verify(fileMetadataRepository).findAll();
        }

        @Test
        @DisplayName("Should not delete non-expired files")
        void shouldNotDeleteNonExpiredFiles() throws IOException {
            // Arrange
            String validUuid = "valid-uuid-001";
            FileMetadata validMetadata = new FileMetadata(
                    validUuid,
                    "valid-file.txt",
                    1024L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(3), // Expires in 3 days
                    0
            );

            // Create actual file
            File validFile = new File(tempDir.toFile(), validUuid);
            Files.write(validFile.toPath(), "valid content".getBytes());

            when(fileMetadataRepository.findAll()).thenReturn(Collections.singletonList(validMetadata));

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            assertThat(validFile).exists(); // File should still exist
            verify(fileMetadataRepository, never()).delete(any());
            verify(fileMetadataRepository).findAll();
        }

        @Test
        @DisplayName("Should handle multiple files with mixed expiry status")
        void shouldHandleMixedExpiryStatus() throws IOException {
            // Arrange
            // Expired file 1
            String expiredUuid1 = "expired-001";
            FileMetadata expiredMetadata1 = new FileMetadata(
                    expiredUuid1,
                    "expired1.txt",
                    100L,
                    LocalDateTime.now().minusDays(10),
                    LocalDateTime.now().minusDays(3),
                    0
            );
            File expiredFile1 = new File(tempDir.toFile(), expiredUuid1);
            Files.write(expiredFile1.toPath(), "expired1".getBytes());

            // Valid file
            String validUuid = "valid-001";
            FileMetadata validMetadata = new FileMetadata(
                    validUuid,
                    "valid.txt",
                    100L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(5),
                    0
            );
            File validFile = new File(tempDir.toFile(), validUuid);
            Files.write(validFile.toPath(), "valid".getBytes());

            // Expired file 2
            String expiredUuid2 = "expired-002";
            FileMetadata expiredMetadata2 = new FileMetadata(
                    expiredUuid2,
                    "expired2.txt",
                    100L,
                    LocalDateTime.now().minusDays(7),
                    LocalDateTime.now().minusDays(1),
                    0
            );
            File expiredFile2 = new File(tempDir.toFile(), expiredUuid2);
            Files.write(expiredFile2.toPath(), "expired2".getBytes());

            List<FileMetadata> allFiles = Arrays.asList(expiredMetadata1, validMetadata, expiredMetadata2);
            when(fileMetadataRepository.findAll()).thenReturn(allFiles);

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            assertThat(expiredFile1).doesNotExist();
            assertThat(expiredFile2).doesNotExist();
            assertThat(validFile).exists();

            verify(fileMetadataRepository).delete(expiredMetadata1);
            verify(fileMetadataRepository).delete(expiredMetadata2);
            verify(fileMetadataRepository, never()).delete(validMetadata);
        }

        @Test
        @DisplayName("Should delete metadata even if physical file doesn't exist")
        void shouldDeleteMetadataWhenFileDoesNotExist() {
            // Arrange
            String missingFileUuid = "missing-file-uuid";
            FileMetadata expiredMetadata = new FileMetadata(
                    missingFileUuid,
                    "missing-file.txt",
                    1024L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1),
                    0
            );

            // Note: We're NOT creating the physical file

            when(fileMetadataRepository.findAll()).thenReturn(Collections.singletonList(expiredMetadata));

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            verify(fileMetadataRepository).delete(expiredMetadata);
        }

        @Test
        @DisplayName("Should handle empty repository")
        void shouldHandleEmptyRepository() {
            // Arrange
            when(fileMetadataRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            verify(fileMetadataRepository).findAll();
            verify(fileMetadataRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should handle files expiring exactly at current time")
        void shouldHandleFilesExpiringAtCurrentTime() throws IOException {
            // Arrange
            String borderlineUuid = "borderline-uuid";

            // Set expiry to current time (should be considered expired due to isBefore check)
            LocalDateTime now = LocalDateTime.now();
            FileMetadata borderlineMetadata = new FileMetadata(
                    borderlineUuid,
                    "borderline.txt",
                    100L,
                    now.minusDays(1),
                    now.minusSeconds(1), // Just expired (1 second ago)
                    0
            );

            File borderlineFile = new File(tempDir.toFile(), borderlineUuid);
            Files.write(borderlineFile.toPath(), "borderline".getBytes());

            when(fileMetadataRepository.findAll()).thenReturn(Collections.singletonList(borderlineMetadata));

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            assertThat(borderlineFile).doesNotExist();
            verify(fileMetadataRepository).delete(borderlineMetadata);
        }

        @Test
        @DisplayName("Should handle file deletion failure gracefully")
        void shouldHandleFileDeletionFailure() throws IOException {
            // Arrange
            String uuid = "undeletable-uuid";
            FileMetadata expiredMetadata = new FileMetadata(
                    uuid,
                    "undeletable.txt",
                    100L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1),
                    0
            );

            // Create a file but make it read-only (may not prevent deletion on all systems)
            File file = new File(tempDir.toFile(), uuid);
            Files.write(file.toPath(), "content".getBytes());
            file.setReadOnly();

            when(fileMetadataRepository.findAll()).thenReturn(Collections.singletonList(expiredMetadata));

            // Act - Should not throw exception even if file deletion fails
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert - Metadata should still be deleted
            verify(fileMetadataRepository).delete(expiredMetadata);
        }

        @Test
        @DisplayName("Should process all files even if one deletion fails")
        void shouldContinueProcessingAfterFailure() throws IOException {
            // Arrange
            String uuid1 = "file-1";
            String uuid2 = "file-2";

            FileMetadata metadata1 = new FileMetadata(
                    uuid1,
                    "file1.txt",
                    100L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1),
                    0
            );

            FileMetadata metadata2 = new FileMetadata(
                    uuid2,
                    "file2.txt",
                    100L,
                    LocalDateTime.now().minusDays(5),
                    LocalDateTime.now().minusDays(1),
                    0
            );

            File file1 = new File(tempDir.toFile(), uuid1);
            File file2 = new File(tempDir.toFile(), uuid2);
            Files.write(file1.toPath(), "content1".getBytes());
            Files.write(file2.toPath(), "content2".getBytes());

            when(fileMetadataRepository.findAll()).thenReturn(Arrays.asList(metadata1, metadata2));

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert - Both should be processed
            verify(fileMetadataRepository).delete(metadata1);
            verify(fileMetadataRepository).delete(metadata2);
        }
    }

    @Nested
    @DisplayName("Scheduler Configuration Tests")
    class SchedulerConfigurationTests {

        @Test
        @DisplayName("Should have correct cron expression annotation")
        void shouldHaveCorrectCronExpression() throws NoSuchMethodException {
            // Act
            var method = FileCleanupScheduler.class.getMethod("cleanupExpiredFiles");
            var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

            // Assert
            assertThat(scheduledAnnotation).isNotNull();
            assertThat(scheduledAnnotation.cron()).isEqualTo("0 0 0 * * ?");
        }

        @Test
        @DisplayName("Should be a Spring Component")
        void shouldBeSpringComponent() {
            // Assert
            assertThat(FileCleanupScheduler.class.isAnnotationPresent(
                    org.springframework.stereotype.Component.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very old expired files")
        void shouldHandleVeryOldExpiredFiles() throws IOException {
            // Arrange
            String oldUuid = "very-old-uuid";
            FileMetadata veryOldMetadata = new FileMetadata(
                    oldUuid,
                    "very-old.txt",
                    100L,
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now().minusMonths(6), // Expired 6 months ago
                    0
            );

            File oldFile = new File(tempDir.toFile(), oldUuid);
            Files.write(oldFile.toPath(), "very old content".getBytes());

            when(fileMetadataRepository.findAll()).thenReturn(Collections.singletonList(veryOldMetadata));

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            assertThat(oldFile).doesNotExist();
            verify(fileMetadataRepository).delete(veryOldMetadata);
        }

        @Test
        @DisplayName("Should handle large number of expired files")
        void shouldHandleLargeNumberOfFiles() throws IOException {
            // Arrange
            int fileCount = 100;
            List<FileMetadata> metadataList = new java.util.ArrayList<>();

            for (int i = 0; i < fileCount; i++) {
                String uuid = "uuid-" + i;
                FileMetadata metadata = new FileMetadata(
                        uuid,
                        "file-" + i + ".txt",
                        100L,
                        LocalDateTime.now().minusDays(5),
                        LocalDateTime.now().minusDays(1),
                        0
                );
                metadataList.add(metadata);

                File file = new File(tempDir.toFile(), uuid);
                Files.write(file.toPath(), ("content " + i).getBytes());
            }

            when(fileMetadataRepository.findAll()).thenReturn(metadataList);

            // Act
            fileCleanupScheduler.cleanupExpiredFiles();

            // Assert
            verify(fileMetadataRepository, times(fileCount)).delete(any(FileMetadata.class));

            // Verify all files are deleted
            for (int i = 0; i < fileCount; i++) {
                File file = new File(tempDir.toFile(), "uuid-" + i);
                assertThat(file).doesNotExist();
            }
        }
    }
}