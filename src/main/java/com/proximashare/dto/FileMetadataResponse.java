package com.proximashare.dto;

import com.proximashare.entity.FileMetadata;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

@Getter
@Setter
public class FileMetadataResponse {
    private String uuid;
    private String filename;
    private long size;
    private LocalDateTime uploadDate;
    private LocalDateTime expiryDate;
    private int downloadCount;
    private boolean isPublic;
    private String ownerUsername;  // Only username, not entire User object
    private String mimeType;

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
        response.mimeType = metadata.getMimeType();

        if (metadata.getOwner() != null && Hibernate.isInitialized(metadata.getOwner())) {
            response.ownerUsername = metadata.getOwner().getUsername();
        } else {
            response.ownerUsername = null;
        }

        return response;
    }

}