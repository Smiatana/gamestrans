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
import com.smiatana.gamestrans.repository.ReleaseRepository;
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
    private final ReleaseRepository releaseRepository;

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
        translation.setDescription(req.getDescription());
        translation.setStatus(req.getStatus());
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
        translation.setDescription(req.getDescription());
        translation.setCreatedBy(currentUser);
        translation.setGame(game);
        translation.setStatus(req.getStatus());
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

    @Transactional
    public Translation update(UUID id, AddTranslationRequest req) throws java.io.IOException {
        Translation translation = findById(id);
        translation.setTitle(req.getTitle());
        translation.setDescription(req.getDescription());
        translation.setStatus(req.getStatus());
        return translationRepository.save(translation);
    }

    @Transactional
    public void delete(UUID id) {
        translationMemberRepository.deleteByTranslationId(id);
        releaseRepository.deleteByTranslationId(id);
        translationRepository.deleteById(id);
    }

    public boolean canEdit(UUID translationId, String email) {
        return translationMemberRepository.existsByTranslationIdAndUserEmail(translationId, email);
    }

    public boolean isOwner(UUID translationId, String email) {
        return translationMemberRepository.existsByTranslationIdAndUserEmailAndRole(translationId, email, "owner");
    }
}
