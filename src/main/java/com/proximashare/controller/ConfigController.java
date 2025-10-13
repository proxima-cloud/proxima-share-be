package com.proximashare.controller;

import com.proximashare.dto.RoleDto;
import com.proximashare.entity.Role;
import com.proximashare.repository.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/config")
@Tag(name = "Config API", description = "Endpoints for fetching static/configuration data")
public class ConfigController {
    private final RoleRepository roleRepository;

    public ConfigController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping("/roles")
    @Operation(summary = "Get all roles", description = "Returns a list of all available roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleDto> roleDtos = roles.stream()
                .map(role -> {
                    RoleDto dto = new RoleDto();
                    dto.setId(role.getId());
                    dto.setName(role.getName());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDtos);
    }
}
