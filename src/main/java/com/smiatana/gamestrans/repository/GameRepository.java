package com.smiatana.gamestrans.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {
    boolean existsByTitle(String title);

    Game findByTitle(String title);
}
