package com.smiatana.gamestrans.service;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.AddTranslationRequest;
import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Genre;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.CommentRepository;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.GenreRepository;
import com.smiatana.gamestrans.repository.IssueRepository;
import com.smiatana.gamestrans.repository.RatingRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TranslationService {
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final FileStorageService fileStorageService;
    private final ReleaseRepository releaseRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;
    private final IssueRepository issueRepository;

    @Transactional
    public Translation create(CreateTranslationRequest req, User currentUser) throws java.io.IOException {
        Game game = new Game();
        game.setTitle(req.getGameTitle());
        game.setDescription(req.getGameDescription());
        game.setCoverUrl(fileStorageService.store(req.getGameCover(), "covers"));
        game.setBackgroundUrl(fileStorageService.store(req.getGameBackground(), "backgrounds"));
        game.setCreatedBy(currentUser);
        game.setDeveloper(req.getDeveloper());
        if (req.getReleaseYear() != null)
            game.setReleaseYear(Year.of(req.getReleaseYear()));
        if (req.getGenres() != null && !req.getGenres().isEmpty())
            game.setGenres(resolveGenres(req.getGenres()));
        gameRepository.save(game);

        Translation translation = new Translation();
        translation.setTitle(req.getTitle());
        translation.setCreatedBy(currentUser);
        translation.setGame(game);
        translation.setDescription(req.getDescription());
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
        translation.setDescription(req.getDescription());
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

    @Transactional
    public Translation update(UUID id, AddTranslationRequest req) throws java.io.IOException {
        Translation translation = findById(id);
        translation.setTitle(req.getTitle());
        translation.setDescription(req.getDescription());

        boolean hasPublishedRelease = releaseRepository.existsByTranslationIdAndStatus(id, "published");
        if (!"draft".equals(req.getStatus()) && !hasPublishedRelease) {
            translation.setStatus("draft");
        } else {
            translation.setStatus(req.getStatus());
        }
        return translationRepository.save(translation);
    }

    @Transactional
    public void delete(UUID id) {
        commentRepository.deleteByTranslationId(id);
        ratingRepository.deleteByTranslationId(id);
        issueRepository.deleteByTranslationId(id);
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