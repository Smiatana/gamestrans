package com.smiatana.gamestrans.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;

import com.smiatana.gamestrans.dto.AddGameRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.GenreRepository;
import com.smiatana.gamestrans.repository.RatingRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.GameService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    private final GenreRepository genreRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final UriService uriService;
    private final RatingRepository ratingRepository;
    private final AuthService authService;

    @GetMapping("/")
    public String index(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) List<UUID> genres,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        User currentUser = authService.getCurrentUser();

        Pageable pageable = PageRequest.of(page, 20, Sort.by("title").ascending());
        String email = currentUser != null ? currentUser.getEmail() : null;
        Page<Game> gamePage = gameService.findVisible(email, search, genres, pageable);

        model.addAttribute("gamePage", gamePage);
        model.addAttribute("games", gamePage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("selectedGenres", genres != null ? genres : List.of());
        model.addAttribute("allGenres", genreRepository.findAllByOrderByNameAsc());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", gamePage.getTotalPages());

        Map<UUID, Boolean> isMemberMap = new HashMap<>();
        Map<UUID, Boolean> hasNonDraftMap = new HashMap<>();

        if (currentUser != null) {
            for (Game g : gamePage.getContent()) {

                boolean isMember = translationMemberRepository
                        .isMemberOfGame(g.getId(), currentUser.getEmail());

                boolean hasNonDraft = translationRepository
                        .hasNonDraftTranslation(g.getId());

                isMemberMap.put(g.getId(), isMember);
                hasNonDraftMap.put(g.getId(), hasNonDraft);
            }
        }

        model.addAttribute("isMemberMap", isMemberMap);
        model.addAttribute("hasNonDraftMap", hasNonDraftMap);
        return "index";
    }

    @GetMapping("/g/{title}")
    public String getGame(@PathVariable String title, Model model) {
        User currentUser = authService.getCurrentUser();

        Game game = gameService.findByTitle(title);

        List<Translation> translations;
        if (currentUser != null) {
            translations = translationRepository.findVisibleByGameId(game.getId(), currentUser.getEmail());
        } else {
            translations = translationRepository.findPublicByGameId(game.getId());
        }

        if (translations.isEmpty() && (currentUser == null ||
                !translationMemberRepository.existsByTranslationGameIdAndUserEmail(
                        game.getId(), currentUser.getEmail()))) {
            return "redirect:/";
        }

        model.addAttribute("game", game);
        model.addAttribute("translations", translations);

        List<UUID> translationIds = translations.stream().map(Translation::getId).toList();

        boolean isOwnerOfAny = false;
        if (currentUser != null) {
            isOwnerOfAny = translationIds.stream()
                    .anyMatch(id -> translationMemberRepository
                            .existsByTranslationIdAndUserEmailAndRole(id, currentUser.getEmail(), "owner"));
        }
        model.addAttribute("isOwnerOfAny", isOwnerOfAny);

        Map<UUID, List<TranslationMember>> membersMap = new HashMap<>();
        for (UUID id : translationIds) {
            membersMap.put(id, translationMemberRepository.findByTranslationId(id));
        }
        model.addAttribute("membersMap", membersMap);

        Map<UUID, Double> ratingsMap = new HashMap<>();
        for (UUID id : translationIds) {
            ratingRepository.findAverageByTranslationId(id)
                    .ifPresent(avg -> ratingsMap.put(id, Math.round(avg * 10.0) / 10.0));
        }
        model.addAttribute("ratingsMap", ratingsMap);

        Map<UUID, Boolean> isMemberMap = new HashMap<>();
        if (currentUser != null) {
            for (UUID id : translationIds) {
                isMemberMap.put(id,
                        translationMemberRepository.existsByTranslationIdAndUserEmail(id, currentUser.getEmail()));
            }
        }
        model.addAttribute("isMemberMap", isMemberMap);

        return "games/show";
    }

    @GetMapping("/g/{gameTitle}/edit")
    public String editPage(@PathVariable String gameTitle, Model model) {
        User currentUser = authService.getCurrentUser();
        Game game = gameService.findByTitle(gameTitle);
        List<Translation> translations = translationRepository.findByGameTitle(gameTitle);
        List<UUID> translationIds = translations.stream().map(Translation::getId).toList();

        boolean isOwnerOfAny = translationIds.stream()
                .anyMatch(id -> translationMemberRepository
                        .existsByTranslationIdAndUserEmailAndRole(id, currentUser.getEmail(), "owner"));
        if (!isOwnerOfAny)
            return "redirect:/g/" + gameTitle;

        AddGameRequest req = new AddGameRequest();
        req.setGameTitle(game.getTitle());
        req.setGameDescription(game.getDescription());
        req.setDeveloper(game.getDeveloper());

        model.addAttribute("game", game);
        model.addAttribute("addGameRequest", req);
        model.addAttribute("allGenres", genreRepository.findAllByOrderByNameAsc());
        return "games/edit";
    }

    @PatchMapping("/g/{gameTitle}/edit")
    public String update(@PathVariable String gameTitle,
            @Valid @ModelAttribute AddGameRequest addGameRequest,
            BindingResult binding,
            Model model) throws java.io.IOException {
        User currentUser = authService.getCurrentUser();
        if (binding.hasErrors()) {
            model.addAttribute("allGenres", genreRepository.findAllByOrderByNameAsc());
            model.addAttribute("game", gameService.findByTitle(gameTitle));
            return "games/edit";
        }
        Game game = gameService.findByTitle(gameTitle);
        List<Translation> translations = translationRepository.findByGameTitle(gameTitle);
        List<UUID> translationIds = translations.stream().map(Translation::getId).toList();
        boolean isOwnerOfAny = translationIds.stream()
                .anyMatch(id -> translationMemberRepository
                        .existsByTranslationIdAndUserEmailAndRole(id, currentUser.getEmail(), "owner"));
        if (!isOwnerOfAny)
            return "redirect:/g/" + gameTitle;
        gameService.update(game.getId(), addGameRequest);
        return "redirect:/g/" + uriService.uri(addGameRequest.getGameTitle());
    }

    @DeleteMapping("/g/{gameTitle}/delete")
    public String deleteGame(@PathVariable String gameTitle) {
        User currentUser = authService.getCurrentUser();
        Game game = gameService.findByTitle(gameTitle);
        List<Translation> translations = translationRepository.findByGameTitle(gameTitle);
        List<UUID> translationIds = translations.stream().map(Translation::getId).toList();
        boolean isOwnerOfAny = translationIds.stream()
                .anyMatch(id -> translationMemberRepository
                        .existsByTranslationIdAndUserEmailAndRole(id, currentUser.getEmail(), "owner"));
        if (!isOwnerOfAny)
            return "redirect:/g/" + gameTitle;
        gameService.delete(game.getId());
        return "redirect:/";
    }
}