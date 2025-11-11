package com.proximashare.utils;

import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.service.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

public class TestAuthHelper {

    public static User createTestUser(String username, String password, String... roleNames) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword(new BCryptPasswordEncoder().encode(password));
        user.setAuthProvider("LOCAL");
        user.setEmail(username + "@test.com");
        user.setEmailVerified(true);
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setId((long) (roleName.hashCode() % 1000));
            role.setName(roleName);
            roles.add(role);
        }
        user.setRoles(roles);

        return user;
    }

    public static String generateToken(JwtService jwtService, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", "ROLE_USER");
        return jwtService.generateToken(claims, username);
    }

    public static String generateTokenWithRoles(JwtService jwtService, String username, String... roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", String.join(",", roles));
        return jwtService.generateToken(claims, username);
    }
}