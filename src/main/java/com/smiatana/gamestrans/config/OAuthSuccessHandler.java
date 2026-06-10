package com.smiatana.gamestrans.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    private String uniqueUsername(String baseUsername) {
        String username = baseUsername;
        int i = 1;
        while (userRepository.existsByUsernameIgnoreCase(username)) {
            username = baseUsername + i++;
        }
        return username;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String avatar = oauthUser.getAttribute("picture");

        if (email == null) {
            throw new IllegalStateException("OAuth login failed: email is null");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            if (userRepository.existsByEmail(email)) {
                user = userRepository.findByEmail(email).orElseThrow();
            } else {
                user = new User();
                user.setEmail(email);
                user.setUsername(uniqueUsername(email.split("@")[0]));
                user.setAvatarUrl(avatar);
                user.setRole("user");
                user.setStatus("active");
                userRepository.save(user);
            }
        }

        if (user.getStatus().equals("frozen")) {
            response.sendRedirect("/unfreeze");
        } else {
            response.sendRedirect("/");
        }
    }

}