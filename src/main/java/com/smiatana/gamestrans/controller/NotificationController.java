package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.smiatana.gamestrans.entity.Notification;
import com.smiatana.gamestrans.repository.NotificationRepository;
import com.smiatana.gamestrans.service.MemberRequestService;
import com.smiatana.gamestrans.service.SseService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NotificationController {
    private final SseService sseService;
    private final MemberRequestService memberRequestService;
    private final NotificationRepository notificationRepository;

    @GetMapping("/notifications")
    public String notificationsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Notification> notifications = notificationRepository
                .findByUserEmailOrderByCreatedAtDesc(userDetails.getUsername());

        model.addAttribute("notifications", notifications);

        return "notifications/index";
    }

    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserDetails userDetails) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseService.register(userDetails.getUsername(), emitter);
        return emitter;
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Notification notification = notificationRepository.findById(id).orElseThrow();
        if (notification.getUser().getEmail().equals(userDetails.getUsername())) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return "redirect:/notifications";
    }

    @PostMapping("/member-requests/{id}/accept")
    public String accept(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        memberRequestService.accept(id, userDetails.getUsername());
        return "redirect:/notifications";
    }

    @PostMapping("/member-requests/{id}/reject")
    public String reject(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        memberRequestService.reject(id, userDetails.getUsername());
        return "redirect:/notifications";
    }
}
