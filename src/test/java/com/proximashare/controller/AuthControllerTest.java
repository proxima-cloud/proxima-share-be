package com.proximashare.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proximashare.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximashare.dto.RegistrationRequest;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.exception.GlobalExceptionHandler;
import com.proximashare.repository.RoleRepository;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.JwtService;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {AuthController.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.environment.production=false",
        "app.error.include-stacktrace=true"
})
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    private Role userRole;
    private Role adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$hashedpassword");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register a new user")
        void shouldRegisterNewUser() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            // Create a user with ID as it would be returned from service
            User savedUser = new User();
            savedUser.setId(1L);
            savedUser.setUsername("newuser");
            savedUser.setPassword("$2a$10$encodedpassword");
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            savedUser.setRoles(roles);

            when(authService.registerUser(any(RegistrationRequest.class))).thenReturn(savedUser);

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("newuser"))
                    .andExpect(jsonPath("$.data.roles[0]").value("USER"));

            verify(authService).registerUser(argThat(req ->
                    req.getUsername().equals("newuser") &&
                            req.getPassword().equals("password123") &&
                            req.getRoles().equals(List.of("USER"))
            ));
        }

        @Test
        @DisplayName("Should register user with multiple roles")
        void shouldRegisterUserWithMultipleRoles() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("adminuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER", "ADMIN"));

            User savedUser = new User();
            savedUser.setId(2L);
            savedUser.setUsername("adminuser");
            savedUser.setPassword("$2a$10$encodedpassword");
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);
            savedUser.setRoles(roles);

            when(authService.registerUser(any(RegistrationRequest.class))).thenReturn(savedUser);

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.id").value(2))
                    .andExpect(jsonPath("$.data.username").value("adminuser"))
                    .andExpect(jsonPath("$.data.roles").isArray())
                    .andExpect(jsonPath("$.data.roles.length()").value(2));

            verify(authService).registerUser(argThat(req ->
                    req.getUsername().equals("adminuser") &&
                            req.getRoles().containsAll(List.of("USER", "ADMIN"))
            ));
        }

        @Test
        @DisplayName("Should return 400 when username is missing")
        void shouldReturn400WhenUsernameIsMissing() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should return 400 when password is missing")
        void shouldReturn400WhenPasswordIsMissing() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setRoles(List.of("USER"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should return 400 when roles are missing")
        void shouldReturn400WhenRolesAreMissing() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should return 400 when username is already taken")
        void shouldReturn400WhenUsernameIsTaken() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("existinguser");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            when(authService.registerUser(any(RegistrationRequest.class)))
                    .thenThrow(new IllegalArgumentException("Username is already taken"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Username is already taken"));

            verify(authService).registerUser(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("Should return 500 when role not found")
        void shouldReturn500WhenRoleNotFound() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("INVALID_ROLE"));

            when(authService.registerUser(any(RegistrationRequest.class)))
                    .thenThrow(new RuntimeException("Role not found: INVALID_ROLE"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("An unexpected server error occurred: Role not found: INVALID_ROLE"));

            verify(authService).registerUser(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("Should return 500 when request body is invalid JSON")
        void shouldReturn500WhenRequestBodyIsInvalidJson() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json}"))
                    .andExpect(status().isInternalServerError());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should return 400 when username is empty string")
        void shouldReturn400WhenUsernameIsEmpty() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordIsTooShort() throws Exception {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("123");
            request.setRoles(List.of("USER"));

            // Act & Assert
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            // Arrange
            Map<String, String> loginRequest = Map.of(
                    "username", "testuser",
                    "password", "password123"
            );

            String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
            when(jwtService.generateToken(anyMap(), eq("testuser"))).thenReturn(token);

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").value(token))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.roles").isArray())
                    .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                    .andExpect(jsonPath("$.message").value("Logged in successfully"));

            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("password123", testUser.getPassword());
            verify(jwtService).generateToken(anyMap(), eq("testuser"));
        }

        @Test
        @DisplayName("Should return 500 when username not found")
        void shouldReturnErrorWhenUsernameNotFound() throws Exception {
            // Arrange
            Map<String, String> loginRequest = Map.of(
                    "username", "nonexistent",
                    "password", "password123"
            );

            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").exists());

            verify(userRepository).findByUsername("nonexistent");
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtService, never()).generateToken(anyMap(), anyString());
        }

        @Test
        @DisplayName("Should return 500 when password is incorrect")
        void shouldReturnErrorWhenPasswordIncorrect() throws Exception {
            // Arrange
            Map<String, String> loginRequest = Map.of(
                    "username", "testuser",
                    "password", "wrongpassword"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", testUser.getPassword())).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").exists());

            verify(userRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("wrongpassword", testUser.getPassword());
            verify(jwtService, never()).generateToken(anyMap(), anyString());
        }

        @Test
        @DisplayName("Should include user roles in JWT token")
        void shouldIncludeRolesInJwtToken() throws Exception {
            // Arrange
            Map<String, String> loginRequest = Map.of(
                    "username", "testuser",
                    "password", "password123"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateToken(anyMap(), anyString())).thenReturn("token");

            // Act
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk());

            // Assert
            verify(jwtService).generateToken(argThat(claims ->
                    claims.containsKey("roles") &&
                            claims.get("roles") instanceof Set
            ), eq("testuser"));
        }

        @Test
        @DisplayName("Should return roles as string list in response")
        void shouldReturnRolesAsStringList() throws Exception {
            // Arrange
            User userWithMultipleRoles = new User();
            userWithMultipleRoles.setUsername("adminuser");
            userWithMultipleRoles.setPassword("$2a$10$hashed");
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);
            userWithMultipleRoles.setRoles(roles);

            Map<String, String> loginRequest = Map.of(
                    "username", "adminuser",
                    "password", "password123"
            );

            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(userWithMultipleRoles));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateToken(anyMap(), anyString())).thenReturn("token");

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roles").isArray())
                    .andExpect(jsonPath("$.data.roles.length()").value(2));
        }

        @Test
        @DisplayName("Should handle missing username in request")
        void shouldHandleMissingUsername() throws Exception {
            // Arrange
            Map<String, String> loginRequest = Map.of(
                    "password", "password123"
            );

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("Should handle missing password in request")
        void shouldHandleMissingPassword() throws Exception {
            // Arrange
            Map<String, String> loginRequest = Map.of(
                    "username", "testuser"
            );

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().is5xxServerError());
        }
    }
}