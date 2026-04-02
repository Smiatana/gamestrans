package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.Translation;

public interface TranslationRepository extends JpaRepository<Translation, UUID> {
    List<Translation> findByGameId(UUID gameId);

    List<Translation> findByGameTitle(String title);
}
