package com.smiatana.gamestrans.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordDigest;

    @Column(nullable = false, unique = true)
    private String username;

    private String bio;
    private String avatarUrl;

    @Column(nullable = false)
    private String role = "user";

    @Column(nullable = false)
    private String status = "pending";

    private LocalDateTime bannedUntil;
    @Column(columnDefinition = "TEXT")
    private String banReason;
    @Column(columnDefinition = "TEXT")
    private String banNote;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime frozenAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isBanned() {
        if (!"banned".equals(status))
            return false;
        if (bannedUntil == null)
            return true; // permanent
        return LocalDateTime.now().isBefore(bannedUntil);
    }

    public boolean isDeleted() {
        return deletedAt != null || "deleted".equals(status);
    }

    public String getDisplayName() {
        if (isDeleted())
            return "<deleted>";
        return username;
    }

}
