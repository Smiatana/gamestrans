package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smiatana.gamestrans.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {
    boolean existsByTitle(String title);

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, UUID id);

    Optional<Game> findByTitle(String title);

    @Query("""
                SELECT DISTINCT g FROM Game g
                WHERE EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
            """)
    List<Game> findPublicGames();

    @Query("""
                SELECT DISTINCT g FROM Game g
                WHERE EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
            """)
    List<Game> findVisibleGames(@Param("email") String email);

    @Query(value = "SELECT DISTINCT g FROM Game g WHERE EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')", countQuery = "SELECT COUNT(DISTINCT g) FROM Game g WHERE EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')")
    Page<Game> findPublicGames(Pageable pageable);

    @Query(value = """
                SELECT DISTINCT g FROM Game g
                WHERE EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
            """, countQuery = """
                SELECT COUNT(DISTINCT g) FROM Game g
                WHERE EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
            """)
    Page<Game> findVisibleGames(@Param("email") String email, Pageable pageable);

    @Query(value = """
                SELECT DISTINCT g FROM Game g
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
            """, countQuery = """
                SELECT COUNT(DISTINCT g) FROM Game g
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
            """)
    Page<Game> findPublicBySearch(@Param("search") String search, Pageable pageable);

    @Query(value = """
                SELECT DISTINCT g FROM Game g
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND (
                    EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                    OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
                )
            """, countQuery = """
                SELECT COUNT(DISTINCT g) FROM Game g
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND (
                    EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                    OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
                )
            """)
    Page<Game> findVisibleBySearch(@Param("email") String email, @Param("search") String search, Pageable pageable);

    @Query(value = """
                SELECT g FROM Game g JOIN g.genres gen
                WHERE gen.id IN :genreIds
                AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """, countQuery = """
                SELECT COUNT(g) FROM Game g JOIN g.genres gen
                WHERE gen.id IN :genreIds
                AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """)
    Page<Game> findPublicByGenres(@Param("genreIds") List<UUID> genreIds, @Param("genreCount") Long genreCount,
            Pageable pageable);

    @Query(value = """
                SELECT g FROM Game g JOIN g.genres gen
                WHERE gen.id IN :genreIds
                AND (
                    EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                    OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
                )
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """, countQuery = """
                SELECT COUNT(g) FROM Game g JOIN g.genres gen
                WHERE gen.id IN :genreIds
                AND (
                    EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                    OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
                )
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """)
    Page<Game> findVisibleByGenres(@Param("email") String email, @Param("genreIds") List<UUID> genreIds,
            @Param("genreCount") Long genreCount, Pageable pageable);

    @Query(value = """
                SELECT g FROM Game g JOIN g.genres gen
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND gen.id IN :genreIds
                AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """, countQuery = """
                SELECT COUNT(g) FROM Game g JOIN g.genres gen
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND gen.id IN :genreIds
                AND EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """)
    Page<Game> findPublicBySearchAndGenres(@Param("search") String search, @Param("genreIds") List<UUID> genreIds,
            @Param("genreCount") Long genreCount, Pageable pageable);

    @Query(value = """
                SELECT g FROM Game g JOIN g.genres gen
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND gen.id IN :genreIds
                AND (
                    EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                    OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
                )
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """, countQuery = """
                SELECT COUNT(g) FROM Game g JOIN g.genres gen
                WHERE LOWER(g.title) LIKE LOWER(CONCAT('%', :search, '%'))
                AND gen.id IN :genreIds
                AND (
                    EXISTS (SELECT t FROM Translation t WHERE t.game = g AND t.status != 'draft')
                    OR EXISTS (SELECT tm FROM TranslationMember tm WHERE tm.translation.game = g AND tm.user.email = :email)
                )
                GROUP BY g HAVING COUNT(DISTINCT gen.id) = :genreCount
            """)
    Page<Game> findVisibleBySearchAndGenres(@Param("email") String email, @Param("search") String search,
            @Param("genreIds") List<UUID> genreIds, @Param("genreCount") Long genreCount, Pageable pageable);

}