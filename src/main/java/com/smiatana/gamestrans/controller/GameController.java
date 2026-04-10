package com.smiatana.gamestrans.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import com.smiatana.gamestrans.dto.AddGameRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.GameService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final UriService uriService;

    @GetMapping("/g")
    public String getGames(Model model) {
        model.addAttribute("games", gameRepository.findAll());
        return "games/index";
    }

    @GetMapping("/g/{title}")
    public String getGame(@PathVariable String title,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("game", gameService.findByTitle(title));
        model.addAttribute("translations", translationRepository.findByGameTitle(title));

        List<Translation> translations = translationRepository.findByGameTitle(title);
        List<UUID> translationIds = translations.stream()
                .map(Translation::getId)
                .toList();

        boolean isOwnerOfAny = false;
        if (userDetails != null) {
            isOwnerOfAny = translationIds.stream()
                    .anyMatch(id -> translationMemberRepository
                            .existsByTranslationIdAndUserEmailAndRole(id, userDetails.getUsername(), "owner"));
        }
        model.addAttribute("isOwnerOfAny", isOwnerOfAny);

        Map<UUID, List<TranslationMember>> membersMap = new HashMap<>();
        for (UUID id : translationIds) {
            membersMap.put(id, translationMemberRepository.findByTranslationId(id));
        }
        model.addAttribute("membersMap", membersMap);
        return "games/show";
    }

    @GetMapping("/g/{gameTitle}/edit")
    public String editPage(@PathVariable String gameTitle,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Game game = gameService.findByTitle(gameTitle);
        List<Translation> translations = translationRepository.findByGameTitle(gameTitle);
        List<UUID> translationIds = translations.stream()
                .map(Translation::getId)
                .toList();

        boolean isOwnerOfAny = translationIds.stream()
                .anyMatch(id -> translationMemberRepository
                        .existsByTranslationIdAndUserEmailAndRole(id, userDetails.getUsername(), "owner"));
        if (!isOwnerOfAny) {
            return "redirect:/g/" + gameTitle;
        }

        AddGameRequest req = new AddGameRequest();
        req.setGameTitle(game.getTitle());
        req.setGameDescription(game.getDescription());

        model.addAttribute("game", game);
        model.addAttribute("addGameRequest", req);
        return "games/edit";
    }

    @PostMapping("/g/{gameTitle}/edit")
    public String update(@PathVariable String gameTitle,
            @Valid @ModelAttribute AddGameRequest addGameRequest,
            BindingResult binding,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "games/edit";
        Game game = gameService.findByTitle(gameTitle);
        gameService.update(game.getId(), addGameRequest);

        String uriGameTitle = uriService.uri(addGameRequest.getGameTitle());
        return "redirect:/g/" + uriGameTitle;
    }

}
