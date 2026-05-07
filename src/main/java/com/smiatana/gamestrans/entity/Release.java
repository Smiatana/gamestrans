package com.smiatana.gamestrans.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "releases", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "translation_id", "title" })
})
@Getter
@Setter
public class Release {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id", nullable = false)
    private Translation translation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;
    @Column(columnDefinition = "text")
    private String fileUrl;

    /**
     * Statuses:
     * - draft: only visible to translation members
     * - on_review: submitted, pending moderator approval
     * - published: approved and public
     * - hidden: approved once but hidden by moderator
     * - deleted: soft-deleted by moderator
     */

    @Column
    private String status = "on_review";

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
