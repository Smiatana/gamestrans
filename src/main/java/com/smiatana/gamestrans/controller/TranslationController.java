package com.smiatana.gamestrans.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.smiatana.gamestrans.dto.AddTranslationRequest;
import com.smiatana.gamestrans.dto.CommentRequest;
import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Comment;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.CommentRepository;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.TranslationService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class TranslationController {
    private final TranslationService translationService;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final GameRepository gameRepository;
    private final UriService uriService;
    private final ReleaseRepository releaseRepository;
    private final CommentRepository commentRepository;

    @GetMapping("/translations/new")
    public String newTranslationPage(Model model) {
        model.addAttribute("createTranslationRequest", new CreateTranslationRequest());
        return "translations/new";
    }

    @PostMapping("/translations/new")
    public String createTranslation(
            @Valid @ModelAttribute CreateTranslationRequest createTranslationRequest,
            BindingResult binding,
            @ModelAttribute("currentUser") User currentUser,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/new";
        var translation = translationService.create(createTranslationRequest, currentUser);

        String uriGameTitle = uriService.uri(translation.getGame().getTitle());
        String uriTransTitle = uriService.uri(translation.getTitle());
        return "redirect:/g/" + uriGameTitle + "/t/"
                + uriTransTitle;
    }

    @GetMapping("/g/{gameTitle}/t/add")
    public String addTranslationPage(@PathVariable String gameTitle, Model model) {
        model.addAttribute("addTranslationRequest", new AddTranslationRequest());
        model.addAttribute("game", gameRepository.findByTitle(gameTitle).orElseThrow());
        return "translations/add";
    }

    @PostMapping("/g/{gameTitle}/t/add")
    public String addTranslation(@PathVariable String gameTitle,
            @Valid @ModelAttribute AddTranslationRequest addTranslationRequest,
            BindingResult binding,
            @ModelAttribute("currentUser") User currentUser,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/add";

        var translation = translationService.add(addTranslationRequest, currentUser, gameTitle);

        String uriGameTitle = uriService.uri(translation.getGame().getTitle());
        String uriTransTitle = uriService.uri(translation.getTitle());
        return "redirect:/g/" + uriGameTitle + "/t/"
                + uriTransTitle;
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}")
    public String translationPage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @ModelAttribute("currentUser") User currentUser, Model model) {

        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();

        if ("draft".equals(translation.getStatus())) {
            boolean isMember = currentUser != null && translationMemberRepository
                    .existsByTranslationIdAndUserEmail(translation.getId(), currentUser.getEmail());
            if (!isMember)
                return "redirect:/g/" + gameTitle;
        }

        List<TranslationMember> members = translationMemberRepository.findByTranslationId(translation.getId());

        List<Release> releases = releaseRepository.findTop5ByTranslationIdOrderByCreatedAtDesc(translation.getId());
        long totalReleases = releaseRepository.countByTranslationId(translation.getId());

        boolean isOwner = false;

        if (currentUser != null) {
            isOwner = translationMemberRepository
                    .existsByTranslationIdAndUserEmailAndRole(translation.getId(), currentUser.getEmail(), "owner");
        }

        boolean isMember = false;
        if (currentUser != null) {
            isMember = translationMemberRepository.existsByTranslationIdAndUserEmailAndRole(translation.getId(),
                    currentUser.getEmail(), "member");
        }

        List<Comment> rootComments = commentRepository
                .findByTranslationIdAndParentIsNullOrderByCreatedAtAsc(translation.getId());

        Map<UUID, List<Comment>> replies = new HashMap<>();
        for (Comment c : rootComments) {
            replies.put(c.getId(), commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId()));
        }

        model.addAttribute("rootComments", rootComments);
        model.addAttribute("replies", replies);
        model.addAttribute("commentRequest", new CommentRequest());

        model.addAttribute("game", game);
        model.addAttribute("translation", translation);
        model.addAttribute("members", members);
        model.addAttribute("releases", releases);
        model.addAttribute("hasMoreReleases", totalReleases > 5);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isMember", isMember);

        return "translations/show";
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}/edit")
    public String editPage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @ModelAttribute("currentUser") User currentUser, Model model) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        List<TranslationMember> members = translationMemberRepository.findByTranslationId(translation.getId());

        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), currentUser.getEmail(), "owner");
        if (!isOwner) {
            return "redirect:/g/" + gameTitle + "/t/" + transTitle;
        }

        AddTranslationRequest req = new AddTranslationRequest();
        req.setTitle(transTitle);
        req.setDescription(translation.getDescription());
        req.setStatus(translation.getStatus());

        model.addAttribute("translation", translation);
        model.addAttribute("addTranslationRequest", req);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("members", members);
        return "translations/edit";
    }

    @PatchMapping("/g/{gameTitle}/t/{transTitle}/edit")
    public String update(@PathVariable String gameTitle, @PathVariable String transTitle,
            @Valid @ModelAttribute AddTranslationRequest addTranslationRequest,
            BindingResult binding,
            @ModelAttribute("currentUser") User currentUser,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/edit";
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        translationService.update(translation.getId(), addTranslationRequest);

        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(addTranslationRequest.getTitle());
        return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;
    }

    @DeleteMapping("/g/{gameTitle}/t/{transTitle}/delete")
    public String delete(@PathVariable String gameTitle, @PathVariable String transTitle,
            @ModelAttribute("currentUser") User currentUser) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), currentUser.getEmail(), "owner");
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);
        if (!isOwner)
            return "redirect:/games/" + uriGameTitle + "/" + uriTransTitle;

        translationService.delete(translation.getId());
        return "redirect:/";
    }

}
