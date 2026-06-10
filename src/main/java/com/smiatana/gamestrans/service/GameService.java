package com.smiatana.gamestrans.service;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.AddGameRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Genre;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.GenreRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final AuthService authService;
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final FileStorageService fileStorageService;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final ReleaseRepository releaseRepository;
    private final AuditLogService auditLogService;

    public Game findById(UUID id) {
        return gameRepository.findById(id).orElseThrow();
    }

    public Game findByTitle(String title) {
        return gameRepository.findByTitle(title).orElseThrow();
    }

    @Transactional
    public Game update(UUID id, AddGameRequest req) throws java.io.IOException {
        Game game = findById(id);
        if (!game.getTitle().equalsIgnoreCase(req.getGameTitle())
                && gameRepository.existsByTitleIgnoreCase(req.getGameTitle())) {
            throw new IllegalArgumentException("Назва гульні ўжо занятая");
        }
        game.setTitle(req.getGameTitle());
        if (req.getGameCover() != null && !req.getGameCover().isEmpty())
            game.setCoverUrl(fileStorageService.store(req.getGameCover(), "covers"));
        if (req.getGameBackground() != null && !req.getGameBackground().isEmpty())
            game.setBackgroundUrl(fileStorageService.store(req.getGameBackground(), "backgrounds"));
        game.setDescription(req.getGameDescription());
        game.setDeveloper(req.getDeveloper());
        game.setReleaseYear(req.getReleaseYear() != null ? Year.of(req.getReleaseYear()) : null);

        if (req.getGenres() != null) {
            game.setGenres(resolveGenres(req.getGenres()));
        } else {
            game.setGenres(new HashSet<>());
        }
        return gameRepository.save(game);
    }

    @Transactional
    public void delete(UUID id) {
        Game game = gameRepository.findById(id).orElseThrow();
        
        // Выдаляем вокладку калі яна ёсць
        if (game.getCoverUrl() != null && !game.getCoverUrl().isBlank()) {
            fileStorageService.delete(game.getCoverUrl());
        }
        
        // Выдаляем фон калі ён ёсць
        if (game.getBackgroundUrl() != null && !game.getBackgroundUrl().isBlank()) {
            fileStorageService.delete(game.getBackgroundUrl());
        }
        
        List<Translation> translations = translationRepository.findByGameId(id);
        for (Translation t : translations) {
            translationMemberRepository.deleteByTranslationId(t.getId());
            releaseRepository.deleteByTranslationId(t.getId());
        }
        translationRepository.deleteByGameId(id);
        gameRepository.deleteById(id);
        
        auditLogService.log(authService.getCurrentUser(), "GAME_DELETE_PERMANENT", "game", id, 
            "Гульня '" + game.getTitle() + "' назаўсёды выдалена");
    }

    public Page<Game> findVisible(String email, String search, List<UUID> genreIds, Pageable pageable) {
        String normalizedSearch = search != null ? search.trim() : "";
        boolean hasSearch = !normalizedSearch.isBlank();
        List<UUID> normalizedGenreIds = genreIds != null ? genreIds : List.of();
        boolean hasGenres = !normalizedGenreIds.isEmpty();

        if (email != null) {
            if (hasSearch && hasGenres)
                return gameRepository.findVisibleBySearchAndGenres(email, normalizedSearch, normalizedGenreIds,
                        (long) normalizedGenreIds.size(), pageable);
            if (hasSearch)
                return gameRepository.findVisibleBySearch(email, normalizedSearch, pageable);
            if (hasGenres)
                return gameRepository.findVisibleByGenres(email, normalizedGenreIds, (long) normalizedGenreIds.size(), pageable);
            return gameRepository.findVisibleGames(email, pageable);
        } else {
            if (hasSearch && hasGenres)
                return gameRepository.findPublicBySearchAndGenres(normalizedSearch, normalizedGenreIds, (long) normalizedGenreIds.size(),
                        pageable);
            if (hasSearch)
                return gameRepository.findPublicBySearch(normalizedSearch, pageable);
            if (hasGenres)
                return gameRepository.findPublicByGenres(normalizedGenreIds, (long) normalizedGenreIds.size(), pageable);
            return gameRepository.findPublicGames(pageable);
        }
    }

    private Set<Genre> resolveGenres(List<String> names) {
        Set<Genre> genres = new HashSet<>();
        for (String name : names) {
            String normalized = name.trim().toLowerCase();
            Genre genre = genreRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> {
                        Genre g = new Genre();
                        g.setName(normalized);
                        return genreRepository.save(g);
                    });
            genres.add(genre);
        }
        return genres;
    }
}