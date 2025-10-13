package com.proximashare.repository;

import com.proximashare.ProximaShareApplication;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import com.proximashare.entity.FileMetadata;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = ProximaShareApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@DisplayName("FileMetadataRepository Integration Tests")
class FileMetadataRepositoryTest {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private EntityManager entityManager;

    private FileMetadata testFile1;
    private FileMetadata testFile2;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        fileMetadataRepository.deleteAll();

        // Create test data
        testFile1 = new FileMetadata(
                "uuid-001",
                "document.pdf",
                1024L,
                LocalDateTime.of(2025, 9, 29, 10, 0),
                LocalDateTime.of(2025, 10, 6, 10, 0),
                0
        );

        testFile2 = new FileMetadata(
                "uuid-002",
                "image.png",
                2048L,
                LocalDateTime.of(2025, 9, 28, 15, 30),
                LocalDateTime.of(2025, 10, 5, 15, 30),
                3
        );
    }

    @Test
    @DisplayName("Should save and retrieve FileMetadata by UUID")
    void shouldSaveAndRetrieveByUuid() {
        // Act
        FileMetadata saved = fileMetadataRepository.save(testFile1);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force DB read

        Optional<FileMetadata> retrieved = fileMetadataRepository.findById("uuid-001");

        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getUuid()).isEqualTo("uuid-001");
        assertThat(retrieved.get().getFilename()).isEqualTo("document.pdf");
        assertThat(retrieved.get().getSize()).isEqualTo(1024L);
        assertThat(retrieved.get().getDownloadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return empty Optional when UUID not found")
    void shouldReturnEmptyWhenUuidNotFound() {
        // Act
        Optional<FileMetadata> result = fileMetadataRepository.findById("non-existent-uuid");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should save multiple FileMetadata entities")
    void shouldSaveMultipleEntities() {
        // Act
        fileMetadataRepository.save(testFile1);
        fileMetadataRepository.save(testFile2);
        entityManager.flush();

        List<FileMetadata> allFiles = fileMetadataRepository.findAll();

        // Assert
        assertThat(allFiles).hasSize(2);
        assertThat(allFiles)
                .extracting(FileMetadata::getUuid)
                .containsExactlyInAnyOrder("uuid-001", "uuid-002");
    }

    @Test
    @DisplayName("Should update existing FileMetadata")
    void shouldUpdateExistingMetadata() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        entityManager.flush();
        entityManager.clear();

        // Act
        FileMetadata toUpdate = fileMetadataRepository.findById("uuid-001").get();
        toUpdate.setDownloadCount(5);
        toUpdate.setFilename("updated-document.pdf");
        fileMetadataRepository.save(toUpdate);
        entityManager.flush();
        entityManager.clear();

        FileMetadata updated = fileMetadataRepository.findById("uuid-001").get();

        // Assert
        assertThat(updated.getDownloadCount()).isEqualTo(5);
        assertThat(updated.getFilename()).isEqualTo("updated-document.pdf");
        assertThat(updated.getSize()).isEqualTo(1024L); // Should remain unchanged
    }

    @Test
    @DisplayName("Should delete FileMetadata by UUID")
    void shouldDeleteByUuid() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        entityManager.flush();

        // Act
        fileMetadataRepository.deleteById("uuid-001");
        entityManager.flush();

        Optional<FileMetadata> deleted = fileMetadataRepository.findById("uuid-001");

        // Assert
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Should check if FileMetadata exists by UUID")
    void shouldCheckExistenceByUuid() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        entityManager.flush();

        // Act & Assert
        assertThat(fileMetadataRepository.existsById("uuid-001")).isTrue();
        assertThat(fileMetadataRepository.existsById("non-existent")).isFalse();
    }

    @Test
    @DisplayName("Should count total FileMetadata records")
    void shouldCountRecords() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        fileMetadataRepository.save(testFile2);
        entityManager.flush();

        // Act
        long count = fileMetadataRepository.count();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle LocalDateTime persistence correctly")
    void shouldPersistLocalDateTimeCorrectly() {
        // Arrange
        LocalDateTime uploadTime = LocalDateTime.of(2025, 9, 29, 14, 30, 45);
        LocalDateTime expiryTime = LocalDateTime.of(2025, 10, 6, 14, 30, 45);

        testFile1.setUploadDate(uploadTime);
        testFile1.setExpiryDate(expiryTime);

        // Act
        fileMetadataRepository.save(testFile1);
        entityManager.flush();
        entityManager.clear();

        FileMetadata retrieved = fileMetadataRepository.findById("uuid-001").get();

        // Assert
        assertThat(retrieved.getUploadDate()).isEqualTo(uploadTime);
        assertThat(retrieved.getExpiryDate()).isEqualTo(expiryTime);
    }

    @Test
    @DisplayName("Should handle large file size values")
    void shouldHandleLargeFileSizes() {
        // Arrange
        testFile1.setSize(1_073_741_824L); // 1GB

        // Act
        fileMetadataRepository.save(testFile1);
        entityManager.flush();
        entityManager.clear();

        FileMetadata retrieved = fileMetadataRepository.findById("uuid-001").get();

        // Assert
        assertThat(retrieved.getSize()).isEqualTo(1_073_741_824L);
    }

    @Test
    @DisplayName("Should handle zero download count")
    void shouldHandleZeroDownloadCount() {
        // Act
        fileMetadataRepository.save(testFile1);
        entityManager.flush();
        entityManager.clear();

        FileMetadata retrieved = fileMetadataRepository.findById("uuid-001").get();

        // Assert
        assertThat(retrieved.getDownloadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should persist and retrieve all fields accurately")
    void shouldPersistAllFieldsAccurately() {
        // Act
        fileMetadataRepository.save(testFile1);
        entityManager.flush();
        entityManager.clear();

        FileMetadata retrieved = fileMetadataRepository.findById("uuid-001").get();

        // Assert - Verify all fields
        assertThat(retrieved.getUuid()).isEqualTo(testFile1.getUuid());
        assertThat(retrieved.getFilename()).isEqualTo(testFile1.getFilename());
        assertThat(retrieved.getSize()).isEqualTo(testFile1.getSize());
        assertThat(retrieved.getUploadDate()).isEqualTo(testFile1.getUploadDate());
        assertThat(retrieved.getExpiryDate()).isEqualTo(testFile1.getExpiryDate());
        assertThat(retrieved.getDownloadCount()).isEqualTo(testFile1.getDownloadCount());
    }

    @Test
    @DisplayName("Should delete all records")
    void shouldDeleteAllRecords() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        fileMetadataRepository.save(testFile2);
        entityManager.flush();

        // Act
        fileMetadataRepository.deleteAll();
        entityManager.flush();

        // Assert
        assertThat(fileMetadataRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle findAll with empty database")
    void shouldHandleFindAllWithEmptyDatabase() {
        // Act
        List<FileMetadata> results = fileMetadataRepository.findAll();

        // Assert
        assertThat(results).isEmpty();
    }
}