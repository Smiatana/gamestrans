package com.smiatana.gamestrans.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.AddTranslationRequest;
import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TranslationService {
    private final GameRepository gameRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public Translation create(CreateTranslationRequest req, User currentUser) throws java.io.IOException {

        Game game = new Game();
        game.setTitle(req.getGameTitle());
        game.setDescription(req.getGameDescription());
        game.setCoverUrl(fileStorageService.store(req.getGameCover(), "covers"));
        game.setCreatedBy(currentUser);
        gameRepository.save(game);

        Translation translation = new Translation();
        translation.setTitle(req.getTitle());
        translation.setCreatedBy(currentUser);
        translation.setGame(game);
        translation.setStatus("draft");
        translationRepository.save(translation);

        TranslationMember member = new TranslationMember();
        member.setTranslation(translation);
        member.setUser(currentUser);
        member.setRole("owner");
        translationMemberRepository.save(member);

        return translation;
    }

    @Transactional
    public Translation add(AddTranslationRequest req, User currentUser, String gameTitle) throws java.io.IOException {
        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();

        Translation translation = new Translation();
        translation.setTitle(req.getTitle());
        translation.setCreatedBy(currentUser);
        translation.setGame(game);
        translation.setStatus("draft");
        translationRepository.save(translation);

        TranslationMember member = new TranslationMember();
        member.setTranslation(translation);
        member.setUser(currentUser);
        member.setRole("owner");
        translationMemberRepository.save(member);
        return translation;
    }

    public Translation findById(UUID id) {
        return translationRepository.findById(id).orElseThrow();
    }

    public List<Translation> findTranslationsByGameTitle(String title) {
        return translationRepository.findByGameTitle(title);
    }

    public Translation update(UUID id, CreateTranslationRequest req) throws java.io.IOException {
        Translation translation = findById(id);
        Game game = translation.getGame();

        game.setTitle(req.getGameTitle());
        game.setDescription(req.getGameDescription());

        game.setCoverUrl(fileStorageService.store(req.getGameCover(), "covers"));
        gameRepository.save(game);
        return translationRepository.save(translation);
    }

    public void delete(UUID id) {
        translationRepository.deleteById(id);
    }
}
