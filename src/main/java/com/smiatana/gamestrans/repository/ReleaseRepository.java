package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smiatana.gamestrans.entity.Release;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

    Optional<Release> findByTranslationIdAndTitle(UUID translationId, String releaseTitle);

    List<Release> findTop5ByTranslationIdOrderByCreatedAtDesc(UUID translationId);

    long countByTranslationId(UUID translationId);

    List<Release> findByTranslationIdOrderByCreatedAtDesc(UUID translationId);

    void deleteByTranslationId(UUID translationId);

    boolean existsByTranslationIdAndStatus(UUID translationId, String status);

    List<Release> findByTranslationIdAndStatusOrderByCreatedAtDesc(UUID translationId, String status);

    /** Pending review queue for moderators */
    Page<Release> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<Release> findByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT r FROM Release r WHERE r.status NOT IN ('deleted') AND r.translation.id = :translationId ORDER BY r.createdAt DESC")
    List<Release> findVisibleByTranslationId(UUID translationId);
}