package com.smiatana.gamestrans.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final UserRepository userRepository;

    @ModelAttribute("currentUser")
    public User currentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        String email = null;

        Object principal = auth.getPrincipal();

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            email = ud.getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth) {
            email = oauth.getAttribute("email");
        }

        if (email == null)
            return null;

        return userRepository.findByEmail(email).orElse(null);
    }
}