package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String email);
}
