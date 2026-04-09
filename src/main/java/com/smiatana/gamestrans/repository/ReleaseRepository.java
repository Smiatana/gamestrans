package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.Release;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {
    List<Release> findByTranslationTitle(UUID translationTitle);

    Optional<Release> findByTranslationIdAndTitle(UUID translationId, String releaseTitle);

    List<Release> findTop5ByTranslationIdOrderByCreatedAtDesc(UUID translationId);

    long countByTranslationId(UUID translationId);

    List<Release> findByTranslationIdOrderByCreatedAtDesc(UUID translationId);

    void deleteByTranslationId(UUID translationId);
}
