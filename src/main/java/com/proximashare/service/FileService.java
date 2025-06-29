package com.proximashare.service;

import com.proximashare.entity.FileMetadata;
import com.proximashare.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileService {
  private final FileMetadataRepository fileMetadataRepository;
  private final String storagePath;

  public FileService(FileMetadataRepository fileMetadataRepository,
      @Value("${file.storage.path}") String storagePath) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.storagePath = storagePath;
  }

  @SuppressWarnings("null")
  public FileMetadata uploadFile(MultipartFile file) {
    if (file.getSize() > 1_073_741_824) { // 1GB limit
      throw new IllegalArgumentException("File size exceeds 1GB");
    }

    if (file.getSize() > 1_250_000) { // 10MB limit
      throw new IllegalArgumentException("File size exceeds 10MB");
    }

    String uuid = UUID.randomUUID().toString();

    while (fileMetadataRepository.existsById(uuid)) {
      uuid = UUID.randomUUID().toString();
    }

    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

    String extension = "";
    if (originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String storedFilename = uuid + extension; // e.g., 550e8400-e29b-41d4-a716-446655440000.txt
    File targetFile = new File(storagePath, storedFilename);

    try {
      file.transferTo(targetFile);
    } catch (Exception e) {
      throw new RuntimeException("Failed to store file", e);
    }

    FileMetadata metadata = new FileMetadata(
        uuid,
        file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
        file.getSize(),
        LocalDateTime.now(),
        LocalDateTime.now().plusDays(3),
        0);

    return fileMetadataRepository.save(metadata);
  }

  public FileMetadata getFileMetadata(String uuid) {
    FileMetadata metadata = fileMetadataRepository.findById(uuid)
        .orElseThrow(() -> new IllegalArgumentException("File not found or expired"));
    if (metadata.getExpiryDate().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("File expired");
    }
    return metadata;
  }

  public File downloadFile(String uuid) {
    FileMetadata metadata = getFileMetadata(uuid);
    metadata.setDownloadCount(metadata.getDownloadCount() + 1);
    fileMetadataRepository.save(metadata);
    String originalFilename = metadata.getFilename();
    String extension = originalFilename.contains(".") ? 
                      originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
    return new File(storagePath, uuid + extension);
  }

}
