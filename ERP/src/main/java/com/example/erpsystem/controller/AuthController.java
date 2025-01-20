package com.example.erpsystem.controller;

import com.example.erpsystem.dto.UserDTO;
import com.example.erpsystem.model.User;
import com.example.erpsystem.repository.UserRepository;
import com.example.erpsystem.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException; // Use standard Java IOException
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${file.upload-dir}")
    private String uploadDir;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@ModelAttribute UserDTO userDTO) {
        System.out.println("Register endpoint hit with: " + userDTO);

        String imageUrl = null;

        // Validate file type and size
        if (userDTO.getImage() != null && !userDTO.getImage().isEmpty()) {
            String fileType = userDTO.getImage().getContentType();
            long fileSize = userDTO.getImage().getSize();
            long maxFileSize = 5 * 1024 * 1024; // 5 MB

            // Check file type
            if (!(fileType.equals("image/jpeg") || fileType.equals("image/png"))) {
                return ResponseEntity.badRequest().body("Only .jpg and .png files are allowed.");
            }

            // Check file size
            if (fileSize > maxFileSize) {
                return ResponseEntity.badRequest().body("File size must be less than 5MB.");
            }

            // Save the file
            try {
                String fileName = System.currentTimeMillis() + "_" + userDTO.getImage().getOriginalFilename();
                Path path = Paths.get(uploadDir);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                }

                Path filePath = path.resolve(fileName);
                userDTO.getImage().transferTo(filePath);
                imageUrl = "/images/" + fileName; // Store the full URL path
            } catch (IOException e) {
                e.printStackTrace(); // Log the error for debugging
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image.");
            }

        }

        // Set the image URL in UserDTO
        userDTO.setImageUrl(imageUrl);

        // Call the service to register the user
        authService.registerUser(userDTO);

        return ResponseEntity.ok("User registered successfully!");
    }

    
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserDTO userDTO) {
        User user = userRepository.findByUsername(userDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password!"));
                
        if (!passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password!");
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful! Welcome " + userDTO.getUsername() + " !!!");
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles().toString());
        // Add any other user details you want to send to the frontend
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        // Clear the security context
        SecurityContextHolder.clearContext();
        
        // Invalidate HTTP session if it exists
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "Logged out successfully");
        
        return ResponseEntity.ok(responseBody);
    }
    
}
