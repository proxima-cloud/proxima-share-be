package com.proximashare.dto;

import com.proximashare.entity.FileMetadata;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

public class FileMetadataResponse {
    private String uuid;
    private String filename;
    private long size;
    private LocalDateTime uploadDate;
    private LocalDateTime expiryDate;
    private int downloadCount;
    private boolean isPublic;
    private String ownerUsername;  // Only username, not entire User object

    public FileMetadataResponse() {
    }

    // Constructor from entity
    public static FileMetadataResponse from(FileMetadata metadata) {
        FileMetadataResponse response = new FileMetadataResponse();
        response.uuid = metadata.getUuid();
        response.filename = metadata.getFilename();
        response.size = metadata.getSize();
        response.uploadDate = metadata.getUploadDate();
        response.expiryDate = metadata.getExpiryDate();
        response.downloadCount = metadata.getDownloadCount();
        response.isPublic = metadata.isPublic();

        if (metadata.getOwner() != null && Hibernate.isInitialized(metadata.getOwner())) {
            response.ownerUsername = metadata.getOwner().getUsername();
        } else {
            response.ownerUsername = null;
        }

        return response;
    }

    // Getters and Setters
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }
}