package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smiatana.gamestrans.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {
    boolean existsByTitle(String title);

    Optional<Game> findByTitle(String title);

    @Query("""
                SELECT DISTINCT g FROM Game g
                WHERE EXISTS (
                    SELECT t FROM Translation t
                    WHERE t.game = g AND t.status != 'draft'
                )
            """)
    List<Game> findPublicGames();

    @Query("""
                SELECT DISTINCT g FROM Game g
                WHERE EXISTS (
                    SELECT t FROM Translation t
                    WHERE t.game = g AND t.status != 'draft'
                ) OR EXISTS (
                    SELECT tm FROM TranslationMember tm
                    WHERE tm.translation.game = g AND tm.user.email = :email
                )
            """)
    List<Game> findVisibleGames(@Param("email") String email);
}
