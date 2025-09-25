package com.proximashare.service.initializers;

import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.RoleRepository;
import com.proximashare.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Service
@DependsOn("roleInitializer")
public class AdminInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Value("${app.admin.username}")
    String adminUsername;

    @Value("${app.admin.password}")
    String adminPassword;

    @PostConstruct
    public void init() {
        // Check if admin user already exists
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);
        if (existingAdmin.isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found!"));

            User adminUser = new User();

            adminUser.setUsername(adminUsername);
            adminUser.setPassword(passwordEncoder.encode(adminPassword)); // default password (change in prod!)
            adminUser.setRoles(Collections.singleton(adminRole));

            userRepository.save(adminUser);
            System.out.println("Default admin user created: username='admin' password='admin123'");
        } else {
            System.out.println("Admin user already exists, skipping creation.");
        }
    }
}
