package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.GameService;
import com.smiatana.gamestrans.service.TranslationService;

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
        return "redirect:/games/" + translation.getGame().getTitle() + "/" + translation.getId();
    }

    @GetMapping("/games/{gameTitle}/{transUuid}")
    public String translationPage(@PathVariable String gameTitle, @PathVariable UUID transUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        Translation translation = translationRepository.findById(transUuid).orElseThrow();
        List<TranslationMember> members = translationMemberRepository.findByTranslationId(transUuid);

        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(transUuid, userDetails.getUsername(), "owner");

        model.addAttribute("game", game);
        model.addAttribute("translation", translation);
        model.addAttribute("members", members);
        model.addAttribute("isOwner", isOwner);

        return "translations/show";
    }

    @GetMapping("/games/{gameTitle}/{id}/edit")
    public String editPage(@PathVariable String gameTitle, @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Translation translation = translationRepository.findById(id).orElseThrow();
        // List<TranslationMember> members =
        // translationMemberRepository.findByTranslationId(id);

        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(id, userDetails.getUsername(), "owner");
        if (!isOwner) {
            return "redirect:/translations/" + id;
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

    @PostMapping("/games/{gameTitle}/{id}/edit")
    public String update(@PathVariable String gameTitle, @PathVariable UUID id,
            @Valid @ModelAttribute CreateTranslationRequest createTranslationRequest,
            BindingResult binding,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/edit";
        translationService.update(id, createTranslationRequest);
        return "redirect:/games/" + gameTitle + "/" + id;
    }

    @PostMapping("/games/{gameTitle}/{id}/delete")
    public String delete(@PathVariable String gameTitle, @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(id, userDetails.getUsername(), "owner");

        if (!isOwner)
            return "redirect:/games/" + gameTitle + "/" + id;

        translationService.delete(id);
        return "redirect:/";
    }

}
