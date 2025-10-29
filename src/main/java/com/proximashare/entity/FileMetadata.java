package com.proximashare.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class FileMetadata {
    @Id
    private String uuid;
    private String filename;
    private long size;
    private String mimeType;
    private LocalDateTime uploadDate;
    private LocalDateTime expiryDate;
    private int downloadCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;  // null for public uploads

    private boolean isPublic;  // true = public, false = user upload

    public FileMetadata() {
    }

    public FileMetadata(String uuid, String filename, long size, LocalDateTime uploadDate, LocalDateTime expiryDate, int downloadCount) {
        this.uuid = uuid;
        this.filename = filename;
        this.size = size;
        this.uploadDate = uploadDate;
        this.expiryDate = expiryDate;
        this.downloadCount = downloadCount;
        this.isPublic = true;  // default for backward compatibility
    }

    // constructor for user uploads
    public FileMetadata(String uuid, String filename, long size, LocalDateTime uploadDate,
                        LocalDateTime expiryDate, int downloadCount, User owner, boolean isPublic) {
        this.uuid = uuid;
        this.filename = filename;
        this.size = size;
        this.uploadDate = uploadDate;
        this.expiryDate = expiryDate;
        this.downloadCount = downloadCount;
        this.owner = owner;
        this.isPublic = isPublic;
    }

    @Override
    public String toString() {
        return "uuid = " + getUuid()
                + "Filename = " + getFilename()
                + "Size = " + getSize()
                + "UploadDate = " + getUploadDate()
                + "ExpiryDate = " + getExpiryDate()
                + "DownloadCount = " + getDownloadCount();
    }

}


