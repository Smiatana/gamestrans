package com.smiatana.gamestrans.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smiatana.gamestrans.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByTranslationIdAndUserId(UUID translationId, UUID userId);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.translation.id = :translationId")
    Optional<Double> findAverageByTranslationId(UUID translationId);

    long countByTranslationId(UUID translationId);

    void deleteByTranslationId(UUID translationId);
}