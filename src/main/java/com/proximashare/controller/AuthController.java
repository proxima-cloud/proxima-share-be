package com.proximashare.controller;

import java.util.*;
import java.util.stream.Collectors;

import com.proximashare.dto.ApiResponse;
import com.proximashare.dto.GoogleLoginRequest;
import com.proximashare.dto.UserData;
import com.proximashare.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proximashare.dto.RegistrationRequest;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.RoleRepository;
import com.proximashare.repository.UserRepository;
import com.proximashare.service.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "This controller provides login and user registration endpoints")
public class AuthController {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Operation(summary = "Register user with role ROLE_USER in 'RegistrationRequest' format",
            description = "Returns UserData of registered user inside 'data' key")
    @PostMapping("/register")
    public ApiResponse<UserData> register(@Valid @RequestBody RegistrationRequest request) {
        User user = authService.registerUser(request);
        UserData userData = UserData.fromUser(user);
        return new ApiResponse<>(userData, "User registered successfully");
    }

    @Operation(summary = "Login registered user with username and password",
            description = "Returns token, id, username and roles inside 'data' key.")
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        User user = userRepo.findByUsername(request.get("username"))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username"));

        if (!passwordEncoder.matches(request.get("password"), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        String token = jwtService.generateToken(Map.of("roles", user.getRoles()), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("roles", user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList()));
        data.put("token", token);

        return new ApiResponse<>(data, "Logged in successfully");
    }

    @Operation(summary = "Login with Google OAuth",
            description = "Automatically creates account on first login. Returns user data and JWT token inside 'data' key")
    @PostMapping("/login/google")
    public ApiResponse<Map<String, Object>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        // Login or create user
        User user = authService.loginOrRegisterGoogleUser(request.getIdToken());

        // Generate JWT token
        String token = jwtService.generateToken(
                Map.of("roles", user.getRoles()),
                user.getUsername()
        );

        // Prepare response
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("profilePictureUrl", user.getProfilePictureUrl());
        data.put("authProvider", user.getAuthProvider());
        data.put("roles", user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList()));
        data.put("token", token);

        return new ApiResponse<>(data, "Logged in successfully with Google");
    }
}
