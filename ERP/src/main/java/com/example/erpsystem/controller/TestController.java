package com.example.erpsystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class TestController {

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "Welcome to the User Dashboard!";
    }
}

