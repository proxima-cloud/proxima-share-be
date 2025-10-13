package com.proximashare.app.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class StorageConfig {

    @Value("${file.storage.path}")
    private String storagePath;

    @PostConstruct
    public void init() {
        File storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            boolean created = storageDir.mkdirs();
            if (created) {
                System.out.println("Created storage directory: " + storageDir.getAbsolutePath());
            } else {
                throw new RuntimeException("Failed to create storage directory: " + storageDir.getAbsolutePath());
            }
        } else {
            System.out.println("Storage directory exists: " + storageDir.getAbsolutePath());
        }
    }
}