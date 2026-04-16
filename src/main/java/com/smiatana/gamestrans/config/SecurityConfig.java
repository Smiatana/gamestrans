package com.smiatana.gamestrans.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import com.smiatana.gamestrans.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
        private final UserRepository userRepository;
        private final OAuthSuccessHandler oauthSuccessHandler;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/g", "/g/**", "/register",
                                                                "/confirm", "/confirm/**",
                                                                "/oauth2/**", "/login/oauth2/**",
                                                                "/login", "/u", "/u/**",
                                                                "/css/**", "/js/**", "/uploads/**")
                                                .permitAll().anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(authSuccessHandler())
                                                .permitAll())

                                .oauth2Login(oauth -> oauth
                                                .loginPage("/login")
                                                .successHandler(oauthSuccessHandler))
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login")
                                                .permitAll());

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

                        if (user.getStatus().equals("frozen")) {
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
                                        boolean enabled = user.getStatus().equals("active")
                                                        || user.getStatus().equals("frozen");

                                        return User.withUsername(user.getEmail())
                                                        .password(user.getPasswordDigest())
                                                        .roles("USER")
                                                        .disabled(!enabled) // THIS blocks pending users
                                                        .build();
                                })
                                .orElseThrow(() -> new UsernameNotFoundException(email));
        }
}
