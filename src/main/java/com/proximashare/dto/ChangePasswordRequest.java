package com.proximashare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Old password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters long")
        String newPassword,

        @NotBlank(message = "Password confirmation is required")
        String confirmNewPassword
) {
}