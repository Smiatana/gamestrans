package com.smiatana.gamestrans.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smiatana.gamestrans.entity.AppSettings;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.AppSettingsRepository;
import com.smiatana.gamestrans.repository.NotificationRepository;
import com.smiatana.gamestrans.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final UserRepository userRepository;
    private final AppSettingsRepository appSettingsRepository;
    private final NotificationRepository notificationRepository;

    @ModelAttribute("currentUser")
    public User currentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser"))
            return null;

        String email = extractEmail(auth.getPrincipal());

        if (email == null)
            return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    @ModelAttribute("appSettings")
    public AppSettings appSettings() {
        return appSettingsRepository.findById(1).orElse(null);
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadCount() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser"))
            return 0;
        String email = extractEmail(auth.getPrincipal());
        if (email == null)
            return 0;
        return notificationRepository.countByUserEmailAndReadFalse(email);
    }

    private String extractEmail(Object principal) {
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud)
            return ud.getUsername();
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth)
            return oauth.getAttribute("email");
        return null;
    }
}