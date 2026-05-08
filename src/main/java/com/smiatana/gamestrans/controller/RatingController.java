package com.smiatana.gamestrans.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.RatingService;
import com.smiatana.gamestrans.repository.TranslationRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;
    private final TranslationRepository translationRepository;
    private final AuthService authService;

    @PostMapping("/g/{gameTitle}/t/{transTitle}/rating")
    public String rate(@PathVariable String gameTitle, @PathVariable String transTitle,
            @RequestParam int stars) {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        ratingService.upsert(translation.getId(), currentUser, stars);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle;
    }

    @DeleteMapping("/g/{gameTitle}/t/{transTitle}/rating")
    public String deleteRating(@PathVariable String gameTitle, @PathVariable String transTitle) {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        ratingService.delete(translation.getId(), currentUser);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle;
    }
}
