package com.proximashare.service;

import com.proximashare.dto.UserProfileResponse;
import com.proximashare.dto.UserStats;
import com.proximashare.entity.FileMetadata;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final String PROFILE_PICTURES_DIR = "uploads/profile-pictures/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int TOKEN_EXPIRY_HOURS = 24; // 24 hours expiry

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        return mapToProfileResponse(user);
    }

    public UserProfileResponse uploadProfilePicture(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(PROFILE_PICTURES_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Delete old profile picture if exists
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
                deleteOldProfilePicture(user.getProfilePictureUrl());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = user.getId() + "_" + UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update user profile picture URL
            String profilePictureUrl = "/uploads/profile-pictures/" + filename;
            user.setProfilePictureUrl(profilePictureUrl);
            User savedUser = userRepository.save(user);

            return mapToProfileResponse(savedUser);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture", e);
        }
    }

    @Transactional
    public void deleteProfilePicture(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            deleteOldProfilePicture(user.getProfilePictureUrl());
            user.setProfilePictureUrl(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public void deactivateAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Account is already deactivated");
        }

        // Soft delete - mark as inactive
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public String generateEmailVerificationToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
            throw new IllegalArgumentException("Email verification is only for LOCAL accounts");
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified");
        }

        // Generate verification token
        String token = UUID.randomUUID().toString();
        user.setEmailVerificationToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token);

        return token;
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (!"LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
            throw new IllegalArgumentException("Email verification is only for LOCAL accounts");
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified");
        }

        // Check if token is expired
        if (user.getTokenExpiryDate() == null || LocalDateTime.now().isAfter(user.getTokenExpiryDate())) {
            throw new IllegalArgumentException("Verification token has expired. Please request a new one.");
        }

        // Mark email as verified
        user.setEmailVerified(true);
        user.setActive(true); // Activate account after email verification
        user.setEmailVerificationToken(null); // Clear token after use
        user.setTokenExpiryDate(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
            throw new IllegalArgumentException("Email verification is only for LOCAL accounts");
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified");
        }

        // Generate new token and send email
        generateEmailVerificationToken(username);
    }

    @Transactional(readOnly = true)
    public UserStats getUserStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        List<FileMetadata> files = user.getUploadedFiles();

        // Calculate total files
        int totalFiles = files != null ? files.size() : 0;

        // Calculate total storage used (sum of all file sizes)
        long totalStorage = files != null
                ? files.stream()
                .mapToLong(FileMetadata::getSize)
                .sum()
                : 0L;

        // Calculate total downloads (sum of all download counts)
        int totalDownloads = files != null
                ? files.stream()
                .mapToInt(FileMetadata::getDownloadCount)
                .sum()
                : 0;

        return UserStats.builder()
                .totalFilesUploaded(totalFiles)
                .totalStorageUsed(totalStorage)
                .totalDownloads(totalDownloads)
                .build();
    }

    private void deleteOldProfilePicture(String profilePictureUrl) {
        try {
            // Extract filename from URL
            String filename = profilePictureUrl.substring(profilePictureUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(PROFILE_PICTURES_DIR, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            // Log the error but don't throw exception
            System.err.println("Failed to delete old profile picture: " + e.getMessage());
        }
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.getEmailVerified())
                .profilePictureUrl(user.getProfilePictureUrl())
                .authProvider(user.getAuthProvider())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
}