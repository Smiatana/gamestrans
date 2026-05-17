package com.smiatana.gamestrans.controller;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    @PostMapping("/subscribe/{type}/{id}")
    public String subscribe(
            @PathVariable String type,
            @PathVariable UUID   id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User current = resolveUser(principal);
        subscriptionService.subscribe(current, type, id);
        return redirectBack(request);
    }

    @PostMapping("/unsubscribe/{type}/{id}")
    public String unsubscribe(
            @PathVariable String type,
            @PathVariable UUID   id,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User current = resolveUser(principal);
        subscriptionService.unsubscribe(current, type, id);
        return redirectBack(request);
    }

    @PostMapping("/subscriptions/remove-subscriber/{subscriberId}")
    public String removeSubscriber(
            @PathVariable UUID subscriberId,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {

        User current = resolveUser(principal);
        subscriptionService.removeSubscriber(current, subscriberId);
        return redirectBack(request);
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}