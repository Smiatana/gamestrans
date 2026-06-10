package com.smiatana.gamestrans.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.smiatana.gamestrans.dto.AddTranslationRequest;
import com.smiatana.gamestrans.dto.CommentRequest;
import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Comment;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.Rating;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.CommentRepository;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.GenreRepository;
import com.smiatana.gamestrans.repository.RatingRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.AuditLogService;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.SubscriptionService;
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
    private final RatingRepository ratingRepository;
    private final GenreRepository genreRepository;
    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final SubscriptionService subscriptionService;

    private boolean isStaff(User user) {
        return user != null &&
                ("admin".equals(user.getRole()) ||
                "moderator".equals(user.getRole()));
    }

    @GetMapping("/translations/new")
    public String newPage(Model model) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null)
            return "redirect:/login";
        model.addAttribute("createTranslationRequest", new CreateTranslationRequest());
        model.addAttribute("allGenres", genreRepository.findAllByOrderByNameAsc());
        return "translations/new";
    }

    @PostMapping("/translations/new")
    public String createTranslation(
            @Valid @ModelAttribute CreateTranslationRequest createTranslationRequest,
            BindingResult binding,
            Model model) throws java.io.IOException {
        User currentUser = authService.getCurrentUser();
        if (gameRepository.existsByTitleIgnoreCase(createTranslationRequest.getGameTitle())) {
            binding.rejectValue("gameTitle", "duplicate", "Гульня з такой назвай ужо існуе");
        }
        if (binding.hasErrors()) {
            return "translations/new";
        }
        try {
            var translation = translationService.create(createTranslationRequest, currentUser);

            String uriGameTitle = uriService.uri(translation.getGame().getTitle());
            String uriTransTitle = uriService.uri(translation.getTitle());
            return "redirect:/g/" + uriGameTitle + "/t/"
                    + uriTransTitle;
        } catch (IllegalArgumentException e) {
            binding.rejectValue("gameTitle", "duplicate", e.getMessage());
            return "translations/new";
        }
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
            Model model) throws java.io.IOException {
        User currentUser = authService.getCurrentUser();
        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        if (translationRepository.existsByGameIdAndTitleIgnoreCase(game.getId(), addTranslationRequest.getTitle())) {
            binding.rejectValue("title", "duplicate", "Назва перакладу ўжо існуе ў гэтай гульні");
        }
        if (binding.hasErrors())
        {
            model.addAttribute("game", game);
            return "translations/add";
        }

        try {
            var translation = translationService.add(addTranslationRequest, currentUser, gameTitle);

            String uriGameTitle = uriService.uri(translation.getGame().getTitle());
            String uriTransTitle = uriService.uri(translation.getTitle());
            return "redirect:/g/" + uriGameTitle + "/t/"
                    + uriTransTitle;
        } catch (IllegalArgumentException e) {
            binding.rejectValue("title", "duplicate", e.getMessage());
            model.addAttribute("game", game);
            return "translations/add";
        }
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}")
    public String translationPage(@PathVariable String gameTitle, @PathVariable String transTitle, Model model) {
        User currentUser = authService.getCurrentUser();
        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();


        if ("draft".equals(translation.getStatus())) {

            boolean canViewDraft = false;

            if (currentUser != null) {
                canViewDraft =
                        isStaff(currentUser) ||
                        translationMemberRepository.existsByTranslationIdAndUserEmail(
                                translation.getId(),
                                currentUser.getEmail()
                        );
            }

            if (!canViewDraft) {
                return "redirect:/g/" + gameTitle;
            }
        }

        List<TranslationMember> members = translationMemberRepository.findByTranslationId(translation.getId());
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

        List<Release> releases;
        boolean canSeeAllReleases = currentUser != null &&
                (isOwner || isMember || isStaff(currentUser));
        
        if (canSeeAllReleases) {
            releases = releaseRepository.findTop5ByTranslationIdOrderByCreatedAtDesc(translation.getId());
        } else {
            releases = releaseRepository.findTop5ByTranslationIdAndStatusOrderByCreatedAtDesc(
                    translation.getId(), "published");
        }

        long totalReleases = releaseRepository.countByTranslationId(translation.getId());

        List<Comment> rootComments = commentRepository
                .findByTranslationIdAndParentIsNullOrderByCreatedAtAsc(translation.getId());
        Map<UUID, List<Comment>> replies = new HashMap<>();
        for (Comment c : rootComments) {
            replies.put(c.getId(), commentRepository.findByParentIdOrderByCreatedAtAsc(c.getId()));
        }

        Optional<Double> avgRating = ratingRepository.findAverageByTranslationId(translation.getId());
        long ratingCount = ratingRepository.countByTranslationId(translation.getId());

        Rating userRating = null;
        if (currentUser != null) {
            userRating = ratingRepository
                    .findByTranslationIdAndUserId(translation.getId(), currentUser.getId())
                    .orElse(null);
        }

        model.addAttribute("game", game);
        model.addAttribute("translation", translation);
        model.addAttribute("members", members);
        model.addAttribute("releases", releases);
        model.addAttribute("hasMoreReleases", totalReleases > 5);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isMember", isMember);

        model.addAttribute("rootComments", rootComments);
        model.addAttribute("replies", replies);
        model.addAttribute("commentRequest", new CommentRequest());

        model.addAttribute("avgRating", avgRating.map(d -> Math.round(d * 10.0) / 10.0).orElse(null));
        model.addAttribute("ratingCount", ratingCount);
        model.addAttribute("userRating", userRating);

        model.addAttribute("translationSubscriberCount",
        subscriptionService.countSubscribersOfTranslation(translation.getId()));
        model.addAttribute("isSubscribedToTranslation",
        subscriptionService.isSubscribed(currentUser,
        SubscriptionService.TYPE_TRANSLATION, translation.getId()));

        return "translations/show";
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}/edit")
    public String editPage(@PathVariable String gameTitle, @PathVariable String transTitle, Model model) {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        List<TranslationMember> members = translationMemberRepository.findByTranslationId(translation.getId());

        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), currentUser.getEmail(), "owner");
        if (!isOwner && !isStaff(currentUser)) {
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
            Model model) throws java.io.IOException {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        if (!translation.getTitle().equalsIgnoreCase(addTranslationRequest.getTitle())
                && translationRepository.existsByGameIdAndTitleIgnoreCaseAndIdNot(
                        translation.getGame().getId(), addTranslationRequest.getTitle(), translation.getId())) {
            binding.rejectValue("title", "duplicate", "Назва перакладу ўжо існуе ў гэтай гульні");
        }
        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), currentUser.getEmail(), "owner");
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(addTranslationRequest.getTitle());
        if (binding.hasErrors()) {
            model.addAttribute("translation", translation);
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("members", translationMemberRepository.findByTranslationId(translation.getId()));
            return "translations/edit";
        }
        if (!isOwner && !isStaff(currentUser)) {
            return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;
        }
        try {
            translationService.update(translation.getId(), addTranslationRequest);
        } catch (IllegalArgumentException e) {
            binding.rejectValue("title", "duplicate", e.getMessage());
            model.addAttribute("translation", translation);
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("members", translationMemberRepository.findByTranslationId(translation.getId()));
            return "translations/edit";
        }

        uriTransTitle = uriService.uri(addTranslationRequest.getTitle());
        
        return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;
    }

    @DeleteMapping("/g/{gameTitle}/t/{transTitle}/delete")
    public String delete(@PathVariable String gameTitle, @PathVariable String transTitle) {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        boolean isOwner = translationMemberRepository
                .existsByTranslationIdAndUserEmailAndRole(translation.getId(), currentUser.getEmail(), "owner");
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);
        if (!isOwner)
            return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;

        translationService.delete(translation.getId());
        auditLogService.log(currentUser, "TRANSLATION_DELETE", "translation", translation.getId(),
            "Пераклад '" + translation.getTitle() + "' выдалены карыстальнікам " + currentUser.getUsername());
        return "redirect:/";
    }

}
