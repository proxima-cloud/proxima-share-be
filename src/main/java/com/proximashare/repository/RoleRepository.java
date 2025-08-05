package com.proximashare.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proximashare.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
