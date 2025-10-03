package com.proximashare.controller;

import java.util.*;
import java.util.stream.Collectors;

import com.proximashare.dto.ApiResponse;
import com.proximashare.dto.UserData;
import com.proximashare.service.AuthService;
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
public class AuthController {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ApiResponse<UserData> register(@Valid @RequestBody RegistrationRequest request) {
        User user = authService.registerUser(request);
        UserData userData = UserData.fromUser(user);
        return new ApiResponse<>(userData,"User registered successfully");
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        User user = userRepo.findByUsername(request.get("username"))
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        if (!passwordEncoder.matches(request.get("password"), user.getPassword())) {
            throw new RuntimeException("Invalid password");
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
}
