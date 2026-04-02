package com.smiatana.gamestrans.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.repository.GameRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    public Game findById(UUID id) {
        return gameRepository.findById(id).orElseThrow();
    }

    public Game findByTitle(String title) {
        return gameRepository.findByTitle(title); // doesn't have orelsethrow but it really should
    }
}
