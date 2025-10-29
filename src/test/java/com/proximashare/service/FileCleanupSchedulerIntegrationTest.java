package com.proximashare.service;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import com.proximashare.ProximaShareApplication;
import com.proximashare.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.proximashare.entity.FileMetadata;
import com.proximashare.repository.FileMetadataRepository;

@SpringBootTest(classes = ProximaShareApplication.class)
@EnableScheduling
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "file.storage.path=${java.io.tmpdir}/scheduler-integration-test"
})
@DisplayName("FileCleanupScheduler Integration Tests")
class FileCleanupSchedulerIntegrationTest {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private FileCleanupScheduler fileCleanupScheduler;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Update storage path using reflection
        var field = FileCleanupScheduler.class.getDeclaredField("storagePath");
        field.setAccessible(true);
        field.set(fileCleanupScheduler, tempDir.toString());

        // Clean up any existing test data
        fileMetadataRepository.deleteAll();
        fileMetadataRepository.flush();
    }

    @AfterEach
    void tearDown() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    @DisplayName("Should cleanup expired files when scheduler runs manually")
    void shouldCleanupExpiredFilesWhenTriggeredManually() throws IOException {
        // Arrange
        String expiredUuid = "integration-expired-uuid";
        String validUuid = "integration-valid-uuid";

        // Create expired file
        FileMetadata expiredMetadata = createFileMetadata(
                expiredUuid,
                "expired-integration.txt",
                100L,
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(1),
                0
        );
        File expiredFile = new File(tempDir.toFile(), expiredUuid);
        Files.write(expiredFile.toPath(), "expired content".getBytes());
        fileMetadataRepository.save(expiredMetadata);

        // Create valid file
        FileMetadata validMetadata = createFileMetadata(
                validUuid,
                "valid-integration.txt",
                100L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3),
                0
        );
        File validFile = new File(tempDir.toFile(), validUuid);
        Files.write(validFile.toPath(), "valid content".getBytes());
        fileMetadataRepository.save(validMetadata);

        assertThat(fileMetadataRepository.count()).isEqualTo(2);
        assertThat(expiredFile).exists();
        assertThat(validFile).exists();

        // Act
        fileCleanupScheduler.cleanupExpiredFiles();

        // Assert
        assertThat(expiredFile).doesNotExist();
        assertThat(validFile).exists();
        assertThat(fileMetadataRepository.count()).isEqualTo(1);
        assertThat(fileMetadataRepository.existsById(validUuid)).isTrue();
        assertThat(fileMetadataRepository.existsById(expiredUuid)).isFalse();
    }

    @Test
    @DisplayName("Should delete multiple expired files in one cleanup run")
    void shouldDeleteMultipleExpiredFiles() throws IOException {
        // Arrange
        FileMetadata expired1 = createAndSaveExpiredFile("exp-1", "expired1.txt");
        FileMetadata expired2 = createAndSaveExpiredFile("exp-2", "expired2.txt");
        FileMetadata expired3 = createAndSaveExpiredFile("exp-3", "expired3.txt");
        FileMetadata valid = createAndSaveValidFile("valid-1", "valid.txt");

        assertThat(fileMetadataRepository.count()).isEqualTo(4);

        // Act
        fileCleanupScheduler.cleanupExpiredFiles();

        // Assert
        assertThat(fileMetadataRepository.count()).isEqualTo(1);
        assertThat(fileMetadataRepository.existsById(valid.getUuid())).isTrue();
        assertThat(fileMetadataRepository.existsById(expired1.getUuid())).isFalse();
        assertThat(fileMetadataRepository.existsById(expired2.getUuid())).isFalse();
        assertThat(fileMetadataRepository.existsById(expired3.getUuid())).isFalse();
    }

    @Test
    @DisplayName("Should handle cleanup with no expired files")
    void shouldHandleNoExpiredFiles() throws IOException {
        // Arrange
        FileMetadata valid1 = createAndSaveValidFile("valid-1", "valid1.txt");
        FileMetadata valid2 = createAndSaveValidFile("valid-2", "valid2.txt");

        long countBefore = fileMetadataRepository.count();

        // Act
        fileCleanupScheduler.cleanupExpiredFiles();

        // Assert
        assertThat(fileMetadataRepository.count()).isEqualTo(countBefore);
        assertThat(fileMetadataRepository.existsById(valid1.getUuid())).isTrue();
        assertThat(fileMetadataRepository.existsById(valid2.getUuid())).isTrue();
    }

    @Test
    @DisplayName("Should persist changes to database after cleanup")
    void shouldPersistChangesAfterCleanup() throws IOException {
        // Arrange
        FileMetadata expired = createAndSaveExpiredFile("exp-db", "expired-db.txt");

        // Act
        fileCleanupScheduler.cleanupExpiredFiles();

        // Assert - Verify data is actually gone from database
        assertThat(fileMetadataRepository.findById(expired.getUuid())).isEmpty();
        assertThat(fileMetadataRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should handle files expiring exactly now")
    void shouldHandleFilesExpiringNow() throws IOException {
        // Arrange
        FileMetadata justExpired = createAndSaveExpiredFile("just-expired", "just-expired.txt",
                LocalDateTime.now().minusSeconds(1));

        assertThat(fileMetadataRepository.count()).isEqualTo(1);

        // Act
        fileCleanupScheduler.cleanupExpiredFiles();

        // Assert
        assertThat(fileMetadataRepository.count()).isEqualTo(0);
    }

    // Helper methods
    private FileMetadata createFileMetadata(String uuid, String filename, Long size,
                                            LocalDateTime uploadDate, LocalDateTime expiryDate,
                                            Integer downloadCount) {
        FileMetadata metadata = new FileMetadata();
        metadata.setUuid(uuid);
        metadata.setFilename(filename);
        metadata.setSize(size);
        metadata.setUploadDate(uploadDate);
        metadata.setExpiryDate(expiryDate);
        metadata.setDownloadCount(downloadCount);
        return metadata;
    }

    private FileMetadata createAndSaveExpiredFile(String uuid, String filename) throws IOException {
        return createAndSaveExpiredFile(uuid, filename, LocalDateTime.now().minusDays(1));
    }

    private FileMetadata createAndSaveExpiredFile(String uuid, String filename,
                                                  LocalDateTime expiryDate) throws IOException {
        FileMetadata metadata = createFileMetadata(
                uuid,
                filename,
                100L,
                LocalDateTime.now().minusDays(5),
                expiryDate,
                0
        );

        File file = new File(tempDir.toFile(), uuid);
        Files.write(file.toPath(), "expired content".getBytes());

        return fileMetadataRepository.save(metadata);
    }

    private FileMetadata createAndSaveValidFile(String uuid, String filename) throws IOException {
        FileMetadata metadata = createFileMetadata(
                uuid,
                filename,
                100L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(3),
                0
        );

        File file = new File(tempDir.toFile(), uuid);
        Files.write(file.toPath(), "valid content".getBytes());

        return fileMetadataRepository.save(metadata);
    }
}