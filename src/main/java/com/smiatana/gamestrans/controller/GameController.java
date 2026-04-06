package com.smiatana.gamestrans.controller;

import org.springframework.stereotype.Controller;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.GameService;

import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;
    private final GameRepository gameRepository;
    private final TranslationRepository translationRepository;

    @GetMapping("/g")
    public String getGames(Model model) {
        model.addAttribute("games", gameRepository.findAll());
        return "games/index";
    }

    @GetMapping("/g/{title}")
    public String getGame(@PathVariable String title, Model model) {
        model.addAttribute("game", gameService.findByTitle(title));
        model.addAttribute("translations", translationRepository.findByGameTitle(title));
        return "games/show";
    }

}
