package com.smiatana.gamestrans.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.RegisterRequest;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    public final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email is taken");
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Name is taken");
        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPasswordDigest(passwordEncoder.encode(req.getPassword()));
        return userRepository.save(user);
    }
}
