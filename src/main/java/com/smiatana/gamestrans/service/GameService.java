package com.smiatana.gamestrans.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.AddGameRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
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
        return game;
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
}
