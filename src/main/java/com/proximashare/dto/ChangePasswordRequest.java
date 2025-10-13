package com.proximashare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Old password is required")
        String oldPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters long")
//        @Pattern(
//                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
//                message = "New password must contain at least one digit, one lowercase, one uppercase, one special character, and no whitespace"
//        ) // commenting for now
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword
) {
}
