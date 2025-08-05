package com.proximashare.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections; // For empty authorities

@Service
public class ApplicationUserDetailsService implements UserDetailsService {

    // In a real application, you'd fetch user details from your database (e.g., via UserRepository)
    // For now, a placeholder for when a valid JWT is presented.
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // If you had a user entity, you'd find it here:
        // UserEntity user = userRepository.findByUsername(username)
        //    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        // return new User(user.getUsername(), user.getPassword(), user.getAuthorities());

        // For JWT validation, we primarily care that the username exists and the token is valid.
        // We don't need a stored password here as authentication is based on the token.
        // The password can be an empty string or a dummy value.
        // The authorities can be loaded from the JWT claims or derived from a user entity.
        return new User(username, "", Collections.emptyList()); // No roles/authorities for now
    }
}