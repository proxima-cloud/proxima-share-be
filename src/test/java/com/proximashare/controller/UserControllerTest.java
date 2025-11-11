package com.proximashare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximashare.config.TestSecurityConfig;
import com.proximashare.dto.ChangePasswordRequest;
import com.proximashare.entity.User;
import com.proximashare.exception.GlobalExceptionHandler;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.JwtService;
import com.proximashare.service.UserService;
import com.proximashare.utils.TestAuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "application.security.jwt.secret=testSecretKeyForJwtTokenGenerationAndValidationTesting123456",
        "application.security.jwt.expiration=3600000",
        "app.environment.production=false",
        "app.error.include-stacktrace=false"
})
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create user with plain password for simplicity in tests
        testUser = TestAuthHelper.createTestUser("testuser", "oldPassword123", "ROLE_USER");

        // Mock password encoder for plain text password (what @WithMockUser provides)
        when(passwordEncoder.matches("oldPassword123", "oldPassword123")).thenReturn(true);
        when(passwordEncoder.matches("wrongOldPassword", "oldPassword123")).thenReturn(false);

        // Mock encoding new passwords
        when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$encodedNewPassword");
        when(passwordEncoder.encode("oldPassword123")).thenReturn("$2a$10$encodedOldPassword");
    }

    @Nested
    @DisplayName("POST /user/change_password")
    class ChangePasswordTests {

        @Test
        @WithMockUser(username = "testuser", password = "oldPassword123", roles = {"USER"})
        @DisplayName("Should change password successfully with valid credentials")
        void shouldChangePasswordSuccessfully() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
//            when(passwordEncoder.matches("oldPassword123", "oldPassword123")).thenReturn(true);
//            when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
//            when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn401WhenNoAuthentication() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should fail when old password is incorrect")
        void shouldFailWhenOldPasswordIncorrect() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "wrongOldPassword",
                    "newPassword456",
                    "newPassword456"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongOldPassword", testUser.getPassword())).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Old password is incorrect"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", password = "oldPassword123", roles = {"USER"})
        @DisplayName("Should fail when new password matches old password")
        void shouldFailWhenNewPasswordMatchesOldPassword() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "oldPassword123",  // Same as old password
                    "oldPassword123"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("oldPassword123")).thenReturn("oldPassword123");
            when(passwordEncoder.matches("oldPassword123", "oldPassword123")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("New and Old passwords cannot be same"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", password = "oldPassword123", roles = {"USER"})
        @DisplayName("Should fail when new password and confirmation don't match")
        void shouldFailWhenNewPasswordAndConfirmationDontMatch() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "differentPassword789"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("oldPassword123")).thenReturn("oldPassword123");
            when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$encodedNewPassword");

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("New password and confirmation do not match"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", password = "oldPassword123", roles = {"USER"})
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should fail validation when old password is blank")
        void shouldFailValidationWhenOldPasswordBlank() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "",
                    "newPassword456",
                    "newPassword456"
            );

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should fail validation when new password is blank")
        void shouldFailValidationWhenNewPasswordBlank() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "",
                    ""
            );

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "testuser", roles = {"USER"})
        @DisplayName("Should fail validation when new password is too short")
        void shouldFailValidationWhenNewPasswordTooShort() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "short",
                    "short"
            );

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @WithMockUser(username = "admin", password = "oldPassword123", roles = {"ADMIN"})
        @DisplayName("Should handle request with admin role")
        void shouldHandleRequestWithAdminRole() throws Exception {
            // Arrange
            User adminUser = TestAuthHelper.createTestUser("admin", "oldPassword123", "ROLE_ADMIN");

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("oldPassword123", adminUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("oldPassword123")).thenReturn("oldPassword123");
            when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));

            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @WithMockUser(username = "testuser", password = "oldPassword123", roles = {"USER"})
        @DisplayName("Should allow USER role to change password")
        void shouldAllowUserRoleToChangePassword() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("oldPassword123")).thenReturn("oldPassword123");
            when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", password = "oldPassword123", roles = {"ADMIN"})
        @DisplayName("Should allow ADMIN role to change password")
        void shouldAllowAdminRoleToChangePassword() throws Exception {
            // Arrange
            User adminUser = TestAuthHelper.createTestUser("admin", "oldPassword123", "ROLE_ADMIN");

            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches("oldPassword123", adminUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("oldPassword123")).thenReturn("oldPassword123");
            when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "guest", roles = {"GUEST"})
        @DisplayName("Should deny access for non-USER/ADMIN roles")
        void shouldDenyAccessForInvalidRoles() throws Exception {
            // Arrange
            ChangePasswordRequest request = new ChangePasswordRequest(
                    "oldPassword123",
                    "newPassword456",
                    "newPassword456"
            );

            // Act & Assert
            mockMvc.perform(post("/user/change_password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).save(any(User.class));
        }
    }
}