package com.example.erpsystem.service;

import com.example.erpsystem.dto.UserDTO;
import com.example.erpsystem.model.Role;
import com.example.erpsystem.model.User;
import com.example.erpsystem.repository.RoleRepository;
import com.example.erpsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(UserDTO userDTO) {
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }

        Role role = roleRepository.findByName(userDTO.getRole());
        if (role == null) {
            throw new RuntimeException("Invalid role!");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setEmail(userDTO.getEmail());
        user.setRoles(Set.of(role));
        user.setImageUrl(userDTO.getImageUrl()); // Set the image URL

        System.out.println("Saving user: " + user.getUsername());

        return userRepository.save(user);
    }

}
