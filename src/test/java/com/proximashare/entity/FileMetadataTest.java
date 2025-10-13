package com.proximashare.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FileMetadata Entity Tests")
class FileMetadataTest {

    @Test
    @DisplayName("Should create FileMetadata with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        // Act
        FileMetadata metadata = new FileMetadata();

        // Assert
        assertNotNull(metadata);
        assertNull(metadata.getUuid());
        assertNull(metadata.getFilename());
        assertEquals(0L, metadata.getSize());
        assertNull(metadata.getUploadDate());
        assertNull(metadata.getExpiryDate());
        assertEquals(0, metadata.getDownloadCount());
    }

    @Test
    @DisplayName("Should create FileMetadata with all-args constructor")
    void shouldCreateWithAllArgsConstructor() {
        // Arrange
        String uuid = "test-uuid-123";
        String filename = "test-file.pdf";
        long size = 1024L;
        LocalDateTime uploadDate = LocalDateTime.now();
        LocalDateTime expiryDate = uploadDate.plusDays(7);
        int downloadCount = 5;

        // Act
        FileMetadata metadata = new FileMetadata(uuid, filename, size, uploadDate, expiryDate, downloadCount);

        // Assert
        assertEquals(uuid, metadata.getUuid());
        assertEquals(filename, metadata.getFilename());
        assertEquals(size, metadata.getSize());
        assertEquals(uploadDate, metadata.getUploadDate());
        assertEquals(expiryDate, metadata.getExpiryDate());
        assertEquals(downloadCount, metadata.getDownloadCount());
    }

    @Test
    @DisplayName("Should correctly set and get all properties")
    void shouldSetAndGetAllProperties() {
        // Arrange
        FileMetadata metadata = new FileMetadata();
        String uuid = "setter-uuid-456";
        String filename = "setter-test.txt";
        long size = 2048L;
        LocalDateTime uploadDate = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        LocalDateTime expiryDate = LocalDateTime.of(2024, 1, 22, 10, 30, 0);
        int downloadCount = 3;

        // Act
        metadata.setUuid(uuid);
        metadata.setFilename(filename);
        metadata.setSize(size);
        metadata.setUploadDate(uploadDate);
        metadata.setExpiryDate(expiryDate);
        metadata.setDownloadCount(downloadCount);

        // Assert
        assertEquals(uuid, metadata.getUuid());
        assertEquals(filename, metadata.getFilename());
        assertEquals(size, metadata.getSize());
        assertEquals(uploadDate, metadata.getUploadDate());
        assertEquals(expiryDate, metadata.getExpiryDate());
        assertEquals(downloadCount, metadata.getDownloadCount());
    }

    @Test
    @DisplayName("Should handle edge cases for size and download count")
    void shouldHandleEdgeCases() {
        // Arrange
        FileMetadata metadata = new FileMetadata();

        // Act & Assert - Test boundary values
        metadata.setSize(0L);
        assertEquals(0L, metadata.getSize());

        metadata.setSize(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, metadata.getSize());

        metadata.setDownloadCount(0);
        assertEquals(0, metadata.getDownloadCount());

        metadata.setDownloadCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, metadata.getDownloadCount());
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValues() {
        // Arrange
        FileMetadata metadata = new FileMetadata();

        // Act - Set null values
        metadata.setUuid(null);
        metadata.setFilename(null);
        metadata.setUploadDate(null);
        metadata.setExpiryDate(null);

        // Assert - Should accept null values without throwing exceptions
        assertNull(metadata.getUuid());
        assertNull(metadata.getFilename());
        assertNull(metadata.getUploadDate());
        assertNull(metadata.getExpiryDate());
    }

    @Test
    @DisplayName("Should maintain data integrity over multiple operations")
    void shouldMaintainDataIntegrity() {
        // Arrange
        FileMetadata metadata = new FileMetadata(
                "original-uuid",
                "original.txt",
                1000L,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                0
        );

        // Act - Simulate file download operations
        String originalUuid = metadata.getUuid();
        String originalFilename = metadata.getFilename();
        long originalSize = metadata.getSize();

        metadata.setDownloadCount(metadata.getDownloadCount() + 1);
        metadata.setDownloadCount(metadata.getDownloadCount() + 1);
        metadata.setDownloadCount(metadata.getDownloadCount() + 1);

        // Assert - Core data should remain unchanged, only download count should change
        assertEquals(originalUuid, metadata.getUuid());
        assertEquals(originalFilename, metadata.getFilename());
        assertEquals(originalSize, metadata.getSize());
        assertEquals(3, metadata.getDownloadCount());
    }
}