package com.smiatana.gamestrans.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smiatana.gamestrans.entity.PasswordResetToken;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.PasswordResetTokenRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.EmailService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final com.smiatana.gamestrans.service.AuditLogService auditLogService;

    @GetMapping("/forgot-password")
    public String forgotPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotSubmit(@RequestParam String email, Model model) {
        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(user.getId()))
                    .filter(t -> t.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5)))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> {
                            },
                            () -> {
                                passwordResetTokenRepository.deleteByUser(user);

                                String token = UUID.randomUUID().toString();
                                PasswordResetToken prt = new PasswordResetToken();
                                prt.setToken(token);
                                prt.setUser(user);
                                prt.setExpiresAt(LocalDateTime.now().plusHours(1));
                                passwordResetTokenRepository.save(prt);

                                emailService.sendPasswordReset(user.getEmail(), token);
                                auditLogService.log(user, "PASSWORD_RESET_REQUEST", "user", user.getId(),
                                    "Запыт на скід пароля адпраўлены карыстальніку " + user.getUsername());
                            });
        });

        model.addAttribute("sent", true);
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPage(@RequestParam String token, Model model) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token).orElse(null);

        if (prt == null || prt.isExpired() || prt.isUsed()) {
            model.addAttribute("error", "Спасылка недзейная або пратэрмінавана. Запытайце новую.");
            return "auth/reset-password";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetSubmit(@RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Паролі не супадаюць");
            return "auth/reset-password";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Пароль павінен быць не менш за 6 сімвалаў");
            return "auth/reset-password";
        }

        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token).orElse(null);
        if (prt == null || prt.isExpired() || prt.isUsed()) {
            model.addAttribute("error", "Спасылка недзейная або пратэрмінавана");
            return "auth/reset-password";
        }

        User user = prt.getUser();
        user.setPasswordDigest(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prt);
        auditLogService.log(user, "PASSWORD_RESET", "user", user.getId(),
            "Пароль абноўлены карыстальнікам " + user.getUsername());

        return "redirect:/login?passwordReset";
    }
}