package com.smiatana.gamestrans.repository;

import com.smiatana.gamestrans.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
    Optional<Genre> findByName(String name);

    List<Genre> findAllByOrderByNameAsc();

    List<Genre> findByNameContainingIgnoreCase(String name);

    Optional<Genre> findByNameIgnoreCase(String name);
}