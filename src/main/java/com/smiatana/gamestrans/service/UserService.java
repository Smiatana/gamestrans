package com.smiatana.gamestrans.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.ChangePasswordRequest;
import com.smiatana.gamestrans.dto.EditProfileRequest;
import com.smiatana.gamestrans.dto.RegisterRequest;
import com.smiatana.gamestrans.entity.ConfirmationToken;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.ConfirmationTokenRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TranslationMemberRepository translationMemberRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;

    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email заняты");
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Імя занятае");
        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPasswordDigest(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);

        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        ConfirmationToken token = new ConfirmationToken();
        token.setToken(code);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        confirmationTokenRepository.save(token);

        emailService.sendConfirmation(user.getEmail(), code);
        return user;
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

    public User changePassword(UUID id, ChangePasswordRequest req) throws IOException {
        User user = userRepository.findById(id).orElseThrow();
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new IllegalArgumentException("Паролі не могуць паўтарацца");
        }
        String oldPasswordDigest = passwordEncoder.encode(req.getOldPassword());
        if (!passwordEncoder.matches(oldPasswordDigest, user.getPasswordDigest())) {
            throw new IllegalArgumentException("Стары пароль не супадае");
        }
        user.setPasswordDigest(passwordEncoder.encode(req.getNewPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        translationMemberRepository.deleteByUserId(id);
        userRepository.deleteById(id);
    }

    public void freeze(UUID id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setStatus("frozen");
        userRepository.save(user);
    }
}
