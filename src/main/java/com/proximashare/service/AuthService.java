package com.proximashare.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.proximashare.dto.RegistrationRequest;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.RoleRepository;
import com.proximashare.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegistrationRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered, Please login or forgot password.");
        }

        String username = request.getUsername();
        String email = request.getEmail();
        String password = request.getPassword();

        List<String> roleNames = request.getRoles();
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            if ("ROLE_ADMIN".equalsIgnoreCase(roleName)) {
                throw new IllegalArgumentException("Admin role cannot be assigned during registration.");
            }

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        }

        // Proceed with registration
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setAuthProvider("LOCAL");
        user.setRoles(roles);
        user.setEmailVerified(false);
        user.setActive(false);
        userRepository.save(user);
        return user;
    }

    @Transactional
    public User loginOrRegisterGoogleUser(String idTokenString) {
        // Verify and decode the token
        Payload payload = verifyGoogleToken(idTokenString);

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String picture = (String) payload.get("picture");

        // Check if user already exists by Google sub ID
        User existingUser = userRepository.findByGoogleId(googleId).orElse(null);
        if (existingUser != null) {
            // User exists, return for login
            return existingUser;
        }

        // Check if email is already used by a local account
        existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            // Email exists with local account but not linked to Google
            throw new IllegalArgumentException("Email is already registered with a local account. Please use password login.");
        }

        // First time login - create new user
        User user = new User();
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setEmailVerified(emailVerified != null ? emailVerified : false);
        user.setUsername(generateUsernameFromEmail(email));
        user.setProfilePictureUrl(picture);
        user.setPassword(null);
        user.setAuthProvider("GOOGLE");
        user.setEmailVerified(true); // Google emails are pre-verified
        user.setActive(true);

        // Assign default ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found: ROLE_USER"));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);
        return user;
    }

    private Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new SecurityException("Invalid Google ID token");
            }

            return idToken.getPayload();
        } catch (Exception e) {
            throw new SecurityException("Failed to verify Google ID token: " + e.getMessage());
        }
    }

    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];

        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}