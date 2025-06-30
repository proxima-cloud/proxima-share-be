package com.proximashare.service;

import java.io.File;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.proximashare.repository.FileMetadataRepository;

@Component
public class FileCleanupScheduler {
  private static final Logger logger = LoggerFactory.getLogger(FileCleanupScheduler.class);
  private final FileMetadataRepository fileMetadataRepository;
  private final String storagePath;

  public FileCleanupScheduler(FileMetadataRepository fileMetadataRepository,
      @Value("${file.storage.path}") String storagePath) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.storagePath = storagePath;
  }

  @Scheduled(cron = "0 0 0 * * ?") // daily at midnight
  public void cleanupExpiredFiles() {
    System.out.println("ProximaShare Auto Delete Utility is started...");
    fileMetadataRepository.findAll().stream()
      .filter(metadata -> metadata.getExpiryDate().isBefore(LocalDateTime.now()))
      .forEach(metadata -> {
        File file = new File(storagePath, metadata.getUuid());
        if (file.exists()) {
          logger.debug("Deleting file: UUID = {}, Filename = {}", metadata.getUuid(), file.getName());
          file.delete();
        }
        fileMetadataRepository.delete(metadata);
      });
    System.out.println("ProximaShare Auto Delete Utility ENDED...");
  }
}