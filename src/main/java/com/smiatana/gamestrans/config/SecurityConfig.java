package com.smiatana.gamestrans.config;

import java.util.List;

import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.unit.DataSize;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;

import jakarta.servlet.MultipartConfigElement;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
        private final UserRepository userRepository;
        private final OAuthSuccessHandler oauthSuccessHandler;
        private final BannedUserFilter bannedUserFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/g", "/g/**", "/register",
                                                                "/confirm", "/confirm/**",
                                                                "/forgot-password", "/reset-password",
                                                                "/oauth2/**", "/login/oauth2/**",
                                                                "/login", "/u", "/u/**",
                                                                "/css/**", "/js/**", "/uploads/**")
                                                .permitAll()
                                                .requestMatchers("/mod", "/mod/**").hasAnyRole("MODERATOR", "ADMIN")
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(authSuccessHandler())
                                                .permitAll())
                                .oauth2Login(oauth -> oauth
                                                .loginPage("/login")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(oauth2UserService()))
                                                .successHandler(oauthSuccessHandler))
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login")
                                                .permitAll())
                                .addFilterBefore(
                                                bannedUserFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationSuccessHandler authSuccessHandler() {
                return (request, response, authentication) -> {
                        UserDetails ud = (UserDetails) authentication.getPrincipal();
                        com.smiatana.gamestrans.entity.User user = userRepository.findByEmail(ud.getUsername())
                                        .orElseThrow();

                        if ("frozen".equals(user.getStatus())) {
                                response.sendRedirect("/unfreeze");
                        } else {
                                response.sendRedirect("/");
                        }
                };
        }

        @Bean
        public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
                return new HiddenHttpMethodFilter();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                return email -> userRepository.findByEmail(email)
                                .map(user -> {
                                        boolean enabled = "active".equals(user.getStatus())
                                                        || "frozen".equals(user.getStatus());

                                        String springRole = switch (user.getRole()) {
                                                case "admin" -> "ADMIN";
                                                case "moderator" -> "MODERATOR";
                                                default -> "USER";
                                        };

                                        System.out.println("Mapped Spring role: " + springRole);

                                        return org.springframework.security.core.userdetails.User
                                                        .withUsername(user.getEmail())
                                                        .password(user.getPasswordDigest() != null
                                                                        ? user.getPasswordDigest()
                                                                        : "")
                                                        .roles(springRole)
                                                        .disabled(!enabled)
                                                        .build();
                                })
                                .orElseThrow(() -> new UsernameNotFoundException(email));
        }

        @Bean
        public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
                return request -> {
                        OAuth2User oauthUser = new DefaultOAuth2UserService().loadUser(request);

                        String email = oauthUser.getAttribute("email");

                        System.out.println("OAuth attributes = " + oauthUser.getAttributes());

                        if (email == null) {
                                throw new IllegalStateException(
                                                "OAuth login failed: email is null. Check provider scopes.");
                        }

                        var user = userRepository.findByEmail(email)
                                        .orElseGet(() -> {
                                                User u = new User();
                                                u.setEmail(email);
                                                u.setUsername(email.split("@")[0]);
                                                u.setStatus("active");
                                                u.setRole("user");
                                                return userRepository.save(u);
                                        });
                        List<GrantedAuthority> authorities = List.of(
                                        new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));

                        return new DefaultOAuth2User(
                                        authorities,
                                        oauthUser.getAttributes(),
                                        "email");
                };
        }
        @Bean
        public MultipartConfigElement multipartConfigElement() {
                MultipartConfigFactory factory = new MultipartConfigFactory();
                factory.setMaxFileSize(DataSize.ofMegabytes(500));
                factory.setMaxRequestSize(DataSize.ofMegabytes(1000));
                return factory.createMultipartConfig();
        }
}