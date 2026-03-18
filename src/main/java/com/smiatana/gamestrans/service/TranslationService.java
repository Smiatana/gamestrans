package com.smiatana.gamestrans.service;

import org.springframework.stereotype.Service;

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

    @Transactional
    public Translation create(CreateTranslationRequest req, User currentUser) throws java.io.IOException {
        String coverUrl = null;

        if (req.getGameCover() != null && !req.getGameCover().isEmpty()) {
            String filename = java.util.UUID.randomUUID() + "_" + req.getGameCover().getOriginalFilename();
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads/covers");
            java.nio.file.Files.createDirectories(uploadDir);
            req.getGameCover().transferTo(uploadDir.resolve(filename));
            coverUrl = "/uploads/covers/" + filename;
        }

        Game game = new Game();
        game.setTitle(req.getGameTitle());
        game.setDescription(req.getGameDescription());
        game.setCoverUrl(coverUrl);
        game.setCreatedBy(currentUser);
        gameRepository.save(game);

        Translation translation = new Translation();
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

}
