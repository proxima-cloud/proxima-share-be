package com.proximashare.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class FileMetadata {
    @Id
    private String uuid;
    private String filename;
    private long size;
    private LocalDateTime uploadDate;
    private LocalDateTime expiryDate;
    private int downloadCount;


    public FileMetadata() {}

    public FileMetadata(String uuid, String filename, long size, LocalDateTime uploadDate, LocalDateTime expiryDate, int downloadCount) {
      this.uuid = uuid;
      this.filename = filename;
      this.size = size;
      this.uploadDate = uploadDate;
      this.expiryDate = expiryDate;
      this.downloadCount = downloadCount;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public int getDownloadCount() { return downloadCount; }
    public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }

}


