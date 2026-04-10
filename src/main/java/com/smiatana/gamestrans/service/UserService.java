package com.smiatana.gamestrans.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.EditProfileRequest;
import com.smiatana.gamestrans.dto.RegisterRequest;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final FileStorageService fileStorageService;
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

    public User update(UUID id, EditProfileRequest req) throws IOException {
        User user = userRepository.findById(id).orElseThrow();
        user.setUsername(req.getUsername());
        if (req.getAvatarCropped() != null && !req.getAvatarCropped().isEmpty()) {
            String url = fileStorageService.storeBase64(req.getAvatarCropped(), "avatars");
            System.out.println(url);
            user.setAvatarUrl(url);
        }
        user.setBio(req.getBio());
        return userRepository.save(user);
    }
}
