package com.smiatana.gamestrans.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FreezeController {
    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/u/{username}/freeze")
    public String freeze(@PathVariable String username,
            @ModelAttribute("currentUser") User currentUser,
            HttpServletRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow();
        boolean isOwner = currentUser != null && user.getEmail().equals(currentUser.getEmail());
        if (!isOwner)
            return "redirect:/u/" + username;

        userService.freeze(user.getId());

        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/login?frozen";
    }

    @GetMapping("/unfreeze")
    public String unfreezePage() {
        return "auth/unfreeze";
    }

    @PatchMapping("/unfreeze")
    public String unfreeze(@ModelAttribute("currentUser") User currentUser) {
        User user = userRepository.findByEmail(currentUser.getEmail()).orElseThrow();
        user.setStatus("active");
        userRepository.save(user);
        return "redirect:/";
    }
}
