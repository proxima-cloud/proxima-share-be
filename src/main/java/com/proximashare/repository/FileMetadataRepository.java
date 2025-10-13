package com.proximashare.repository;

import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {
    // Find all files for a specific user
    List<FileMetadata> findByOwnerOrderByUploadDateDesc(User owner);

    // Find file by UUID and owner (for ownership verification)
    Optional<FileMetadata> findByUuidAndOwner(String uuid, User owner);

    // Existing method for cleanup
    List<FileMetadata> findByExpiryDateBefore(LocalDateTime dateTime);

    // Fetch owner eagerly when finding by UUID
    @Query("SELECT f FROM FileMetadata f LEFT JOIN FETCH f.owner WHERE f.uuid = :uuid")
    Optional<FileMetadata> findByIdWithOwner(String uuid);

}
