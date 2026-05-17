package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.smiatana.gamestrans.entity.Notification;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.NotificationRepository;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.MemberRequestService;
import com.smiatana.gamestrans.service.SseService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NotificationController {
    private final SseService sseService;
    private final MemberRequestService memberRequestService;
    private final NotificationRepository notificationRepository;
    private final AuthService authService;
    private final com.smiatana.gamestrans.service.AuditLogService auditLogService;

    @GetMapping("/notifications")
    public String notificationsPage(@ModelAttribute("currentUser") User currentUser, Model model) {
        List<Notification> notifications = notificationRepository
                .findByUserEmailOrderByCreatedAtDesc(currentUser.getEmail());

        model.addAttribute("notifications", notifications);

        return "notifications/index";
    }

    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        User currentUser = authService.getCurrentUser();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseService.register(currentUser.getEmail(), emitter);
        return emitter;
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        Notification notification = notificationRepository.findById(id).orElseThrow();
        if (notification.getUser().getEmail().equals(currentUser.getEmail())) {
            notification.setRead(true);
            notificationRepository.save(notification);
            auditLogService.log(currentUser, "NOTIFICATION_READ", "notification", id,
                    "Апавяшчэнне прачытана карыстальнікам " + currentUser.getUsername());
        }
        return "redirect:/notifications";
    }

    @PostMapping("/member-requests/{id}/accept")
    public String accept(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        memberRequestService.accept(id, currentUser.getEmail());
        return "redirect:/notifications";
    }

    @PostMapping("/member-requests/{id}/reject")
    public String reject(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        memberRequestService.reject(id, currentUser.getEmail());
        return "redirect:/notifications";
    }
}
