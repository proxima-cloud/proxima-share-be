package com.proximashare.service;

import com.proximashare.app.config.FileUploadConfig;
import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.User;
import com.proximashare.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {
    private final FileMetadataRepository fileMetadataRepository;
    private final FileUploadConfig uploadConfig;
    private final String storagePath;

    public FileService(FileMetadataRepository fileMetadataRepository,
                       FileUploadConfig uploadConfig,
                       @Value("${file.storage.path}") String storagePath) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.uploadConfig = uploadConfig;
        this.storagePath = storagePath;
    }

    @SuppressWarnings("null")
    public FileMetadata uploadFile(MultipartFile file) {
        if (file.getSize() > uploadConfig.getPublicMaxSize()) {
            throw new IllegalArgumentException("File size exceeds " +
                    (uploadConfig.getPublicMaxSize() / 1_073_741_824) + "GB limit");
        }

        String uuid = generateUniqueUuid();
        String originalFilename = getOriginalFilename(file);
        String storedFilename = uuid + getFileExtension(originalFilename);

        saveFileToDisk(file, storedFilename);

        FileMetadata metadata = new FileMetadata(
                uuid,
                originalFilename,
                file.getSize(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(uploadConfig.getPublicExpiryDays()),
                0);
        metadata.setPublic(true);  // Mark as public

        return fileMetadataRepository.save(metadata);
    }

    // Authenticated User Upload
    @SuppressWarnings("null")
    public FileMetadata uploadFileForUser(MultipartFile file, User user) {
        if (file.getSize() > uploadConfig.getUserMaxSize()) {
            throw new IllegalArgumentException("File size exceeds " +
                    (uploadConfig.getUserMaxSize() / 1_073_741_824) + "GB limit");
        }

        String uuid = generateUniqueUuid();
        String originalFilename = getOriginalFilename(file);
        String storedFilename = uuid + getFileExtension(originalFilename);

        saveFileToDisk(file, storedFilename);

        FileMetadata metadata = new FileMetadata(
                uuid,
                originalFilename,
                file.getSize(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(uploadConfig.getUserExpiryDays()),
                0,
                user,
                false);  // Not public, owned by user

        return fileMetadataRepository.save(metadata);
    }

    // Get all files for a user
    public List<FileMetadata> getUserFiles(User user) {
        return fileMetadataRepository.findByOwnerOrderByUploadDateDesc(user);
    }

    // Delete file with ownership check
    public void deleteUserFile(String uuid, User user) throws FileNotFoundException {
        FileMetadata metadata = fileMetadataRepository.findByUuidAndOwner(uuid, user)
                .orElseThrow(() -> new FileNotFoundException("File not found or you don't own this file"));

        // Delete physical file
        String extension = getFileExtension(metadata.getFilename());
        File physicalFile = new File(storagePath, uuid + extension);
        if (physicalFile.exists()) {
            physicalFile.delete();
        }

        // Delete metadata
        fileMetadataRepository.delete(metadata);
    }

    // Get metadata (works for both public and user files)
    public FileMetadata getFileMetadata(String uuid) throws FileNotFoundException {
        FileMetadata metadata = fileMetadataRepository.findByIdWithOwner(uuid)
                .orElseThrow(() -> new FileNotFoundException("File not found or expired"));

        if (metadata.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("File expired");
        }

        return metadata;
    }

    // Download with download limit check
    public File downloadFile(String uuid) throws FileNotFoundException, IllegalAccessException {
        FileMetadata metadata = getFileMetadata(uuid);

        // Different limits for public vs user files
        int maxDownloads = metadata.isPublic() ?
                uploadConfig.getPublicMaxDownloads() :
                uploadConfig.getUserMaxDownloads();

        if (metadata.getDownloadCount() >= maxDownloads) {
            throw new IllegalAccessException("File download limit reached for this file. (Max. " + maxDownloads + " Times)");
        }

        metadata.setDownloadCount(metadata.getDownloadCount() + 1);
        fileMetadataRepository.save(metadata);

        String extension = getFileExtension(metadata.getFilename());
        return new File(storagePath, uuid + extension);
    }

    // HELPER METHODS
    private String generateUniqueUuid() {
        String uuid = UUID.randomUUID().toString();
        while (fileMetadataRepository.existsById(uuid)) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    private String getOriginalFilename(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return "unknown";
        }
        return originalFilename;
    }

    private String getFileExtension(String filename) {
        if (filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private void saveFileToDisk(MultipartFile file, String storedFilename) {
        File targetFile = new File(storagePath, storedFilename);
        try {
            file.transferTo(targetFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}