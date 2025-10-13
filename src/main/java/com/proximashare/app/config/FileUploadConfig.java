package com.proximashare.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadConfig {

    private long publicMaxSize = 1_073_741_824L;  // 1GB default
    private int publicExpiryDays = 7;
    private int publicMaxDownloads = 3;

    private long userMaxSize = 5_368_709_120L;  // 5GB default
    private int userExpiryDays = 30;
    private int userMaxDownloads = 100;  // Higher limit for authenticated users

    // Getters and Setters
    public long getPublicMaxSize() {
        return publicMaxSize;
    }

    public void setPublicMaxSize(long publicMaxSize) {
        this.publicMaxSize = publicMaxSize;
    }

    public int getPublicExpiryDays() {
        return publicExpiryDays;
    }

    public void setPublicExpiryDays(int publicExpiryDays) {
        this.publicExpiryDays = publicExpiryDays;
    }

    public int getPublicMaxDownloads() {
        return publicMaxDownloads;
    }

    public void setPublicMaxDownloads(int publicMaxDownloads) {
        this.publicMaxDownloads = publicMaxDownloads;
    }

    public long getUserMaxSize() {
        return userMaxSize;
    }

    public void setUserMaxSize(long userMaxSize) {
        this.userMaxSize = userMaxSize;
    }

    public int getUserExpiryDays() {
        return userExpiryDays;
    }

    public void setUserExpiryDays(int userExpiryDays) {
        this.userExpiryDays = userExpiryDays;
    }

    public int getUserMaxDownloads() {
        return userMaxDownloads;
    }

    public void setUserMaxDownloads(int userMaxDownloads) {
        this.userMaxDownloads = userMaxDownloads;
    }
}