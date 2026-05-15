package com.smiatana.gamestrans.repository;

import com.smiatana.gamestrans.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    Optional<Genre> findByName(String name);

    List<Genre> findAllByOrderByNameAsc();

    @Query("""
            SELECT DISTINCT gen FROM Game g JOIN g.genres gen
            WHERE g.deletedAt IS NULL
            AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
            ORDER BY gen.name ASC
            """)
    List<Genre> findAllByVisibleGames();

    List<Genre> findByNameContainingIgnoreCase(String name);

    Optional<Genre> findByNameIgnoreCase(String name);
}