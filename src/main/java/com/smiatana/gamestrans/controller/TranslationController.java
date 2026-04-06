package com.smiatana.gamestrans.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.TranslationService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TranslationController {
    private final TranslationService translationService;
    private final UserRepository userRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final GameRepository gameRepository;
    private final UriService uriService;
    private final ReleaseRepository releaseRepository;

    @GetMapping("/translations/new")
    public String newTranslationPage(Model model) {
        model.addAttribute("createTranslationRequest", new CreateTranslationRequest());
        return "translations/new";
    }

    @PostMapping("/translations/new")
    public String createTranslation(
            @Valid @ModelAttribute CreateTranslationRequest createTranslationRequest,
            BindingResult binding,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/new";

        User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        var translation = translationService.create(createTranslationRequest, currentUser);

        String uriGameTitle = uriService.uri(translation.getGame().getTitle());
        String uriTransTitle = uriService.uri(translation.getTitle());
        return "redirect:/games/" + uriGameTitle + "/"
                + uriTransTitle;
    }

    @GetMapping("/games/{gameTitle}/{transTitle}")
    public String translationPage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {

        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        List<TranslationMember> members = translationMemberRepository.findByTranslationId(translation.getId());

        List<Release> releases = releaseRepository.findTop5ByTranslationIdOrderByCreatedAtDesc(translation.getId());
        long totalReleases = releaseRepository.countByTranslationId(translation.getId());

        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), userDetails.getUsername(), "owner");

        model.addAttribute("game", game);
        model.addAttribute("translation", translation);
        model.addAttribute("members", members);
        model.addAttribute("releases", releases);
        model.addAttribute("hasMoreReleases", totalReleases > 5);
        model.addAttribute("isOwner", isOwner);

        return "translations/show";
    }

    @GetMapping("/games/{gameTitle}/{transTitle}/edit")
    public String editPage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        // List<TranslationMember> members =
        // translationMemberRepository.findByTranslationId(id);

        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), userDetails.getUsername(), "owner");
        if (!isOwner) {
            return "redirect:/games/" + gameTitle + "/" + transTitle;
        }

        CreateTranslationRequest req = new CreateTranslationRequest();
        req.setGameTitle(translation.getGame().getTitle());
        req.setGameDescription(translation.getGame().getDescription());
        req.setDescription(translation.getDescription());

        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();

        model.addAttribute("game", game);
        model.addAttribute("translation", translation);
        model.addAttribute("createTranslationRequest", req);
        return "translations/edit";
    }

    @PutMapping("/games/{gameTitle}/{transTitle}/edit")
    public String update(@PathVariable String gameTitle, @PathVariable String transTitle,
            @Valid @ModelAttribute CreateTranslationRequest createTranslationRequest,
            BindingResult binding,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/edit";
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        translationService.update(translation.getId(), createTranslationRequest);

        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);
        return "redirect:/games/" + uriGameTitle + "/" + uriTransTitle;
    }

    @DeleteMapping("/games/{gameTitle}/{transTitle}/delete")
    public String delete(@PathVariable String gameTitle, @PathVariable String transTitle,
            @AuthenticationPrincipal UserDetails userDetails) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), userDetails.getUsername(), "owner");
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);
        if (!isOwner)
            return "redirect:/games/" + uriGameTitle + "/" + uriTransTitle;

        translationService.delete(translation.getId());
        return "redirect:/";
    }

}
