package com.proximashare.controller;

import com.proximashare.dto.ApiResponse;
import com.proximashare.dto.ChangePasswordRequest;
import com.proximashare.dto.UserData;
import com.proximashare.entity.User;
import com.proximashare.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
}
