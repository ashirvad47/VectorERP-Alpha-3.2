package com.example.erpsystem.controller;

import com.example.erpsystem.service.IdentityCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/identity-cards")
public class IdentityCardController {

    private final IdentityCardService identityCardService;

    public IdentityCardController(IdentityCardService identityCardService) {
        this.identityCardService = identityCardService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<String> generateIdentityCard(@PathVariable Long userId) {
        String message = identityCardService.generateIdentityCard(userId);
        return ResponseEntity.ok(message);
    }
}
