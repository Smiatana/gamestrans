package com.smiatana.gamestrans.service;

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
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final FileStorageService fileStorageService;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final ReleaseRepository releaseRepository;

    public Game findById(UUID id) {
        return gameRepository.findById(id).orElseThrow();
    }

    public Game findByTitle(String title) {
        return gameRepository.findByTitle(title).orElseThrow();
    }

    @Transactional
    public Game update(UUID id, AddGameRequest req) throws java.io.IOException {
        Game game = findById(id);
        game.setTitle(req.getGameTitle());
        if (req.getGameCover() != null && !req.getGameCover().isEmpty()) {
            game.setCoverUrl(fileStorageService.store(req.getGameCover(), "covers"));
        }
        game.setDescription(req.getGameDescription());
        game.setDeveloper(req.getDeveloper());
        if (req.getGenres() != null) {
            Set<Genre> genres = new HashSet<>();

            if (req.getGenres() != null) {
                for (String name : req.getGenres()) {
                    String normalized = name.trim().toLowerCase();

                    Genre genre = genreRepository.findByNameIgnoreCase(normalized)
                            .orElseGet(() -> {
                                Genre g = new Genre();
                                g.setName(normalized);
                                return genreRepository.save(g);
                            });

                    genres.add(genre);
                }
            }

            game.setGenres(genres);
        } else {
            game.setGenres(new HashSet<>());
        }
        return gameRepository.save(game);
    }

    @Transactional
    public void delete(UUID id) {
        List<Translation> translations = translationRepository.findByGameId(id);
        for (Translation t : translations) {
            translationMemberRepository.deleteByTranslationId(t.getId());
            releaseRepository.deleteByTranslationId(t.getId());
        }
        translationRepository.deleteByGameId(id);
        gameRepository.deleteById(id);
    }

    public Page<Game> findVisible(String email, String search, List<UUID> genreIds, Pageable pageable) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasGenres = genreIds != null && !genreIds.isEmpty();

        if (email != null) {
            if (hasSearch && hasGenres)
                return gameRepository.findVisibleBySearchAndGenres(email, search.trim(), genreIds,
                        (long) genreIds.size(), pageable);
            if (hasSearch)
                return gameRepository.findVisibleBySearch(email, search.trim(), pageable);
            if (hasGenres)
                return gameRepository.findVisibleByGenres(email, genreIds, (long) genreIds.size(), pageable);
            return gameRepository.findVisibleGames(email, pageable);
        } else {
            if (hasSearch && hasGenres)
                return gameRepository.findPublicBySearchAndGenres(search.trim(), genreIds, (long) genreIds.size(),
                        pageable);
            if (hasSearch)
                return gameRepository.findPublicBySearch(search.trim(), pageable);
            if (hasGenres)
                return gameRepository.findPublicByGenres(genreIds, (long) genreIds.size(), pageable);
            return gameRepository.findPublicGames(pageable);
        }
    }
}