package com.proximashare.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import com.proximashare.ProximaShareApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proximashare.dto.RegistrationRequest;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.RoleRepository;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.JwtService;

@SpringBootTest(classes = ProximaShareApplication.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        // Ensure USER role exists (should be created by RoleInitializer)
        userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("USER");
                    return roleRepository.save(role);
                });
    }

    @AfterEach
    void tearDown() {
        // Clean up test users (roles are managed by initializers)
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should complete full registration and login flow")
    void shouldCompleteRegistrationAndLoginFlow() throws Exception {
        // Step 1: Register a new user
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("integrationuser");
        registrationRequest.setPassword("password123");
        registrationRequest.setRoles(Arrays.asList("USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("integrationuser"))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        // Verify user was saved to database
        User savedUser = userRepository.findByUsername("integrationuser").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("integrationuser");
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();

        // Step 2: Login with the registered user
        Map<String, String> loginRequest = Map.of(
                "username", "integrationuser",
                "password", "password123"
        );

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.username").value("integrationuser"))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andReturn();

        // Step 3: Verify JWT token is valid
        String responseBody = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        String token = (String) ((Map<String, Object>) response.get("data")).get("token");

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // Verify token can extract username
        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("integrationuser");
    }

    @Test
    @DisplayName("Should prevent duplicate username registration")
    void shouldPreventDuplicateUsername() throws Exception {
        // Register first user
        RegistrationRequest request1 = new RegistrationRequest();
        request1.setUsername("duplicateuser");
        request1.setPassword("password123");
        request1.setRoles(Arrays.asList("USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());
        long count1 = userRepository.count();

        // Try to register with same username
        RegistrationRequest request2 = new RegistrationRequest();
        request2.setUsername("duplicateuser");
        request2.setPassword("differentpassword");
        request2.setRoles(Arrays.asList("USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());

        // Verify only one user exists
        long count2 = userRepository.count();
        assertThat(count1).isEqualTo(count2);
    }

    @Test
    @DisplayName("Should hash passwords correctly")
    void shouldHashPasswordsCorrectly() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("hasheduser");
        request.setPassword("plainpassword123");
        request.setRoles(Arrays.asList("USER"));

        // Act
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Assert
        User user = userRepository.findByUsername("hasheduser").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("plainpassword123");
        assertThat(user.getPassword()).startsWith("$2a$"); // BCrypt hash prefix
        assertThat(passwordEncoder.matches("plainpassword123", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Should fail login with incorrect password")
    void shouldFailLoginWithIncorrectPassword() throws Exception {
        // Register user
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("secureuser");
        registrationRequest.setPassword("correctpassword");
        registrationRequest.setRoles(Arrays.asList("USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isOk());

        // Try to login with wrong password
        Map<String, String> loginRequest = Map.of(
                "username", "secureuser",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should generate unique JWT tokens for different users")
    void shouldGenerateUniqueTokensForDifferentUsers() throws Exception {
        // Register two users
        RegistrationRequest user1Request = new RegistrationRequest();
        user1Request.setUsername("user1");
        user1Request.setPassword("password123");
        user1Request.setRoles(Arrays.asList("USER"));

        RegistrationRequest user2Request = new RegistrationRequest();
        user2Request.setUsername("user2");
        user2Request.setPassword("password123");
        user2Request.setRoles(Arrays.asList("USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isOk());

        // Login as user1
        Map<String, String> login1 = Map.of("username", "user1", "password", "password123");
        MvcResult result1 = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andExpect(status().isOk())
                .andReturn();

        // Login as user2
        Map<String, String> login2 = Map.of("username", "user2", "password", "password123");
        MvcResult result2 = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract tokens
        String token1 = JsonPath.parse(result1.getResponse().getContentAsString())
                .read("$.data.token");

        String token2 = JsonPath.parse(result2.getResponse().getContentAsString())
                .read("$.data.token");

        // Verify tokens are different and contain correct usernames
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtService.extractUsername(token1)).isEqualTo("user1");
        assertThat(jwtService.extractUsername(token2)).isEqualTo("user2");
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void shouldHandleSpecialCharactersInUsername() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("user_123-test");
        request.setPassword("password123");
        request.setRoles(Arrays.asList("USER"));

        // Act
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Login
        Map<String, String> loginRequest = Map.of(
                "username", "user_123-test",
                "password", "password123"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user_123-test"));
    }

    @Test
    @DisplayName("Should persist user roles correctly")
    void shouldPersistUserRolesCorrectly() throws Exception {
        // Register user with USER role
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("roleuser");
        request.setPassword("password123");
        request.setRoles(Arrays.asList("USER"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Retrieve and verify roles
        User user = userRepository.findByUsername("roleuser").orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles().iterator().next().getName()).isEqualTo("USER");
    }
}