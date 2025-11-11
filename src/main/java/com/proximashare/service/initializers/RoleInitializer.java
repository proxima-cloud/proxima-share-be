package com.proximashare.service.initializers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.proximashare.entity.Role;
import com.proximashare.repository.RoleRepository;

import jakarta.annotation.PostConstruct;

@Service
@Profile("!test")
public class RoleInitializer {
    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);
        }
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
        }
    }
}
