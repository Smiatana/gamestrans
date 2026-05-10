package com.smiatana.gamestrans.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BannedUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {

            String email = null;

            Object principal = auth.getPrincipal();

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                email = ud.getUsername();
            } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth) {
                email = oauth.getAttribute("email");
            }

            if (email != null) {
                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null && "banned".equals(user.getStatus())) {

                    request.getSession().invalidate();

                    SecurityContextHolder.clearContext();

                    response.sendRedirect("/login?banned");

                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}