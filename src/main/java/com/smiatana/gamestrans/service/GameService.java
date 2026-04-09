package com.smiatana.gamestrans.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.AddGameRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.repository.GameRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final FileStorageService fileStorageService;

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
}
