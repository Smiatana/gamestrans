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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String avatar = oauthUser.getAttribute("picture");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            if (userRepository.existsByEmail(email)) {
                user = userRepository.findByEmail(email).orElseThrow();
            } else {
                user = new User();
                user.setEmail(email);
                String baseUsername = email.split("@")[0];
                String username = baseUsername;
                int i = 1;
                while (userRepository.existsByUsername(username)) {
                    username = baseUsername + i++;
                }
                user.setUsername(username);
                user.setAvatarUrl(avatar);
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