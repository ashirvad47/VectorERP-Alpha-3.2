package com.example.erpsystem.controller;

import com.example.erpsystem.model.Role;
import com.example.erpsystem.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostMapping
    public ResponseEntity<String> addRole(@RequestBody Role role) {
        if (roleRepository.findByName(role.getName()) != null) {
            return ResponseEntity.badRequest().body("Role already exists!");
        }
        roleRepository.save(role);
        return ResponseEntity.ok("Role added successfully!");
    }

    @GetMapping
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(roleRepository.findAll());
    }
}
