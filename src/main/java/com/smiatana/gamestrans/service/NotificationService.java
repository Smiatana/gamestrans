package com.smiatana.gamestrans.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.Notification;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void send(User user, String type, Map<String, String> payload) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setPayload(objectMapper.writeValueAsString(payload));
        notificationRepository.save(notification);
        sseService.send(user.getEmail(), type, notification.getPayload());
    }
}
