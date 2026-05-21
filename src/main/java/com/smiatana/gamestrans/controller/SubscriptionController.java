package com.smiatana.gamestrans.controller;

import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final AuthService authService;

    @PostMapping("/subscribe/{type}/{id}")
    public String subscribe(
            @PathVariable String type,
            @PathVariable UUID   id,
            HttpServletRequest request) {

        User current = authService.getCurrentUser();
        if (current == null) {
            return "redirect:/login";
        }
        subscriptionService.subscribe(current, type, id);
        return redirectBack(request);
    }

    @PostMapping("/unsubscribe/{type}/{id}")
    public String unsubscribe(
            @PathVariable String type,
            @PathVariable UUID   id,
            HttpServletRequest request) {

        User current = authService.getCurrentUser();
        if (current == null) {
            return "redirect:/login";
        }
        subscriptionService.unsubscribe(current, type, id);
        return redirectBack(request);
    }

    @PostMapping("/subscriptions/remove-subscriber/{subscriberId}")
    public String removeSubscriber(
            @PathVariable UUID subscriberId,
            HttpServletRequest request) {

        User current = authService.getCurrentUser();
        if (current == null) {
            return "redirect:/login";
        }
        subscriptionService.removeSubscriber(current, subscriberId);
        return redirectBack(request);
    }

    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}