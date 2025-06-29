package com.proximashare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.proximashare.repository.FileMetadataRepository;

import java.io.File;
import java.time.LocalDateTime;

@Component
public class FileCleanupScheduler {
    private final FileMetadataRepository fileMetadataRepository;
    private final String storagePath;

    public FileCleanupScheduler(FileMetadataRepository fileMetadataRepository, 
                               @Value("${file.storage.path}") String storagePath) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.storagePath = storagePath;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    public void cleanupExpiredFiles() {
        fileMetadataRepository.findAll().stream()
            .filter(metadata -> metadata.getExpiryDate().isBefore(LocalDateTime.now()))
            .forEach(metadata -> {
                File file = new File(storagePath, metadata.getUuid());
                if (file.exists()) {
                    file.delete();
                }
                fileMetadataRepository.delete(metadata);
            });
    }
}