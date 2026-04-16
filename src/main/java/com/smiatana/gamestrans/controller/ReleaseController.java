package com.smiatana.gamestrans.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;

import com.smiatana.gamestrans.dto.CreateReleaseRequest;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.ReleaseService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ReleaseController {
    private final ReleaseService releaseService;
    private final UserRepository userRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final UriService uriService;
    private final ReleaseRepository releaseRepository;

    @GetMapping("/releases/new")
    public String newReleasePage(@RequestParam(required = false) UUID translationId,
            @ModelAttribute("currentUser") User currentUser, Model model) {
        List<TranslationMember> members = translationMemberRepository.findByUserEmail(currentUser.getEmail());
        List<Translation> translations = members.stream()
                .map(TranslationMember::getTranslation)
                .toList();

        CreateReleaseRequest req = new CreateReleaseRequest();
        if (translationId != null)
            req.setTranslationId(translationId);

        model.addAttribute("translations", translations);
        model.addAttribute("createReleaseRequest", req);
        return "releases/new";
    }

    @PostMapping("releases/new")
    public String createReleaseGlobalPage(@Valid @ModelAttribute CreateReleaseRequest createReleaseRequest,
            BindingResult binding,
            @ModelAttribute("currentUser") User currentUser, Model model) throws IOException {
        if (binding.hasErrors()) {
            List<TranslationMember> members = translationMemberRepository.findByUserEmail(currentUser.getEmail());
            model.addAttribute("translations", members.stream().map(TranslationMember::getTranslation).toList());
            return "/releases/new";
        }
        Translation translation = translationRepository.findById(createReleaseRequest.getTranslationId()).orElseThrow();
        var release = releaseService.create(createReleaseRequest, currentUser, translation);
        String uriGameTitle = uriService.uri(translation.getGame().getTitle());
        String uriTransTitle = uriService.uri(translation.getTitle());
        String uriReleaseTitle = uriService.uri(release.getTitle());

        return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle + "/r/"
                + uriReleaseTitle;
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}/r/{releaseTitle}")
    public String releasePage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable String releaseTitle, @ModelAttribute("currentUser") User currentUser, Model model) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        boolean isMember = false;
        if (currentUser != null) {
            isMember = translationMemberRepository.existsByTranslationIdAndUserEmail(translation.getId(),
                    currentUser.getEmail());
        }

        Release release = releaseRepository.findByTranslationIdAndTitle(translation.getId(), releaseTitle)
                .orElseThrow();
        model.addAttribute("isMember", isMember);
        model.addAttribute("release", release);
        model.addAttribute("translation", translation);
        return "releases/show";
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}/r")
    public String releasesPage(@PathVariable String gameTitle, @PathVariable String transTitle, Model model) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        List<Release> releases = releaseRepository.findByTranslationIdOrderByCreatedAtDesc(translation.getId());
        model.addAttribute("translation", translation);
        model.addAttribute("releases", releases);
        return "releases/index";
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}/r/{releaseTitle}/edit")
    public String editPage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable String releaseTitle,
            @ModelAttribute("currentUser") User currentUser, Model model) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        boolean isMember = translationMemberRepository.existsByTranslationIdAndUserEmail(translation.getId(),
                currentUser.getEmail());
        if (!isMember) {
            return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/r/" + releaseTitle;
        }
        Release release = releaseRepository.findByTranslationIdAndTitle(translation.getId(), releaseTitle)
                .orElseThrow();
        CreateReleaseRequest req = new CreateReleaseRequest();
        req.setTitle(release.getTitle());
        req.setDescription(release.getDescription());
        req.setReleaseLink(release.getFileUrl());
        model.addAttribute("release", release);
        model.addAttribute("createReleaseRequest", req);
        return "releases/edit";
    }

    @PatchMapping("/g/{gameTitle}/t/{transTitle}/r/{releaseTitle}/edit")
    public String update(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable String releaseTitle, @Valid @ModelAttribute CreateReleaseRequest createReleaseRequest,
            @ModelAttribute("currentUser") User currentUser, Model model) throws IOException {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        Release release = releaseRepository.findByTranslationIdAndTitle(translation.getId(), releaseTitle)
                .orElseThrow();

        releaseService.update(release.getId(), createReleaseRequest);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/r/" + createReleaseRequest.getTitle();
    }

}
