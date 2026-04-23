package com.smiatana.gamestrans.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.smiatana.gamestrans.dto.RegisterRequest;
import com.smiatana.gamestrans.entity.ConfirmationToken;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.ConfirmationTokenRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.EmailService;
import com.smiatana.gamestrans.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @GetMapping("/register")

    public String registerPage(org.springframework.ui.Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest, BindingResult binding, Model model) {
        if (binding.hasErrors())
            return "auth/register";
        try {
            userService.register(registerRequest);
            return "redirect:/confirm?email=" + registerRequest.getEmail();
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/confirm")
    public String confirmPage() {
        return "auth/confirm";
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String email,
            @RequestParam String code,
            Model model) {

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            model.addAttribute("error", "Карыстальнік не знойдзены");
            return "auth/confirm";
        }

        ConfirmationToken token = confirmationTokenRepository
                .findByToken(code)
                .orElse(null);

        if (token == null || token.isExpired() || !token.getUser().getId().equals(user.getId())) {
            model.addAttribute("error", "Няправільны або пратэрмінаваны код");
            return "auth/confirm";
        }

        user.setStatus("active");
        userRepository.save(user);

        confirmationTokenRepository.delete(token);

        return "redirect:/login?confirmed";
    }

    @PostMapping("/confirm/resend")
    public String resend(@RequestParam String email, Model model) {
        User user = userRepository.findByEmail(email).orElseThrow();

        ConfirmationToken lastToken = confirmationTokenRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElse(null);

        if (lastToken != null &&
                lastToken.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1))) {

            model.addAttribute("error", "Пачакайце 1 хвіліну перад паўторнай адпраўкай");
            return "auth/confirm";
        }

        String code = String.valueOf((int) (Math.random() * 900000) + 100000);

        ConfirmationToken token = new ConfirmationToken();
        token.setToken(code);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        confirmationTokenRepository.save(token);

        emailService.sendConfirmation(user.getEmail(), code);

        return "redirect:/confirm?email=" + email;
    }

}
