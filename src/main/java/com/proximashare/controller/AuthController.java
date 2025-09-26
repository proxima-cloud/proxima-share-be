package com.proximashare.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegistrationRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        List<String> roleNames = request.getRoles();

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepo.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        userRepo.save(user);
        return "User registered";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        User user = userRepo.findByUsername(request.get("username"))
                .orElseThrow(() -> new RuntimeException("Invalid username"));
        if (!passwordEncoder.matches(request.get("password"), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(Map.of("roles", user.getRoles()), user.getUsername());
        return Map.of(
            "token", token,
            "username", user.getUsername(),
            "roles", user.getRoles()
                    .stream()
                    .map(r -> r.getName())
                    .collect(Collectors.toList())
        );
    }
}
