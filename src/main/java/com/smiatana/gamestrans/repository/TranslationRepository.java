package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smiatana.gamestrans.entity.Translation;

public interface TranslationRepository extends JpaRepository<Translation, UUID> {
    List<Translation> findByGameId(UUID gameId);

    List<Translation> findByGameTitle(String gameTitle);

    Optional<Translation> findByGameTitleAndTitle(String gameTitle, String translationTitle);

    void deleteByGameId(UUID gameId);

    @Query("""
                SELECT t FROM Translation t
                WHERE t.game.id = :gameId
                AND (t.status != 'draft' OR EXISTS (
                    SELECT tm FROM TranslationMember tm
                    WHERE tm.translation = t AND tm.user.email = :email
                ))
            """)
    List<Translation> findVisibleByGameId(@Param("gameId") UUID gameId, @Param("email") String email);

    @Query("""
                SELECT t FROM Translation t
                WHERE t.game.id = :gameId
                AND t.status != 'draft'
            """)
    List<Translation> findPublicByGameId(@Param("gameId") UUID gameId);

}
