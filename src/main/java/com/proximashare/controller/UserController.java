package com.proximashare.controller;

import com.proximashare.dto.*;
import com.proximashare.entity.User;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    /**
     * Get current user profile details
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserProfileResponse profile = userService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(profile, "Profile retrieved successfully"));
    }

    /**
     * Upload or update profile picture
     */
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file
    ) {
        UserProfileResponse updatedProfile = userService.uploadProfilePicture(
                userDetails.getUsername(),
                file
        );
        return ResponseEntity.ok(new ApiResponse<>(updatedProfile, "Profile picture uploaded successfully"));
    }

    /**
     * Delete profile picture
     */
    @DeleteMapping("/profile-picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteProfilePicture(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.deleteProfilePicture(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(null, "Profile picture deleted successfully"));
    }

    /**
     * Change user password
     */
    @PostMapping("/change_password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if old password matches
        if (!passwordEncoder.matches(request.oldPassword(), userDetails.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        // Check if old and new password matches
        if (userDetails.getPassword().equals(encodedPassword)) {
            throw new IllegalArgumentException("New and Old passwords cannot be same");
        }

        // Check if new password matches confirmation
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        // Update password
        user.setPassword(encodedPassword);
        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>(null, "Password changed successfully"));
    }

    /**
     * Deactivate user account (soft delete)
     */
    @PostMapping("/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(null, "Account deactivated successfully"));
    }

    /**
     * Send email verification token (for LOCAL accounts only)
     */
    @PostMapping("/send-verification-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> sendVerificationEmail(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.resendVerificationEmail(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(null, "Verification email sent successfully"));
    }

    /**
     * Verify email with token (public endpoint)
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        userService.verifyEmail(request.getToken());
        return ResponseEntity.ok(new ApiResponse<>(null, "Email verified successfully"));
    }

    /**
     * Get account statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserStats>> getUserStats(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserStats stats = userService.getUserStats(userDetails.getUsername());
        return ResponseEntity.ok(new ApiResponse<>(stats, "Statistics retrieved successfully"));
    }
}
