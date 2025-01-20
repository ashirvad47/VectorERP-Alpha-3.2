package com.example.erpsystem.service;

import com.example.erpsystem.model.User;
import com.example.erpsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User saveUser(User user) {
        return userRepository.save(user); // Saves the user to the database, including imageUrl
    }
}
