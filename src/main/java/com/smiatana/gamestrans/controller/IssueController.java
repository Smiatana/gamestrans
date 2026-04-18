package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smiatana.gamestrans.dto.IssueRequest;
import com.smiatana.gamestrans.entity.Issue;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.IssueRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.IssueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class IssueController {
    private final IssueService issueService;
    private final IssueRepository issueRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;

    @GetMapping("/g/{gameTitle}/t/{transTitle}/issues")
    public String index(@PathVariable String gameTitle, @PathVariable String transTitle,
            @ModelAttribute("currentUser") User currentUser, Model model) {
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        List<Issue> issues = issueRepository
                .findByTranslationIdOrderByCreatedAtDesc(translation.getId());

        boolean isMember = currentUser != null && translationMemberRepository
                .existsByTranslationIdAndUserEmail(translation.getId(), currentUser.getEmail());

        model.addAttribute("translation", translation);
        model.addAttribute("issues", issues);
        model.addAttribute("isMember", isMember);
        model.addAttribute("issueRequest", new IssueRequest());
        return "issues/index";
    }

    @PostMapping("/g/{gameTitle}/t/{transTitle}/issues")
    public String create(@PathVariable String gameTitle, @PathVariable String transTitle,
            @Valid @ModelAttribute IssueRequest issueRequest,
            BindingResult binding,
            @ModelAttribute("currentUser") User currentUser, Model model) {
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        if (binding.hasErrors()) {
            model.addAttribute("translation", translation);
            model.addAttribute("issues", issueRepository
                    .findByTranslationIdOrderByCreatedAtDesc(translation.getId()));
            return "issues/index";
        }
        issueService.create(issueRequest, currentUser, translation.getId());
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/issues";
    }

    @GetMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}/edit")
    public String editPage(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable UUID issueId,
            @ModelAttribute("currentUser") User currentUser, Model model) {
        Issue issue = issueRepository.findById(issueId).orElseThrow();
        if (!issue.getAuthor().getId().equals(currentUser.getId()))
            return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/issues";

        IssueRequest req = new IssueRequest();
        req.setTitle(issue.getTitle());
        req.setDescription(issue.getDescription());
        req.setReleaseTitle(issue.getReleaseTitle());
        model.addAttribute("issue", issue);
        model.addAttribute("issueRequest", req);
        model.addAttribute("translation", translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow());
        return "issues/edit";
    }

    @PatchMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}")
    public String update(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable UUID issueId,
            @Valid @ModelAttribute IssueRequest issueRequest,
            BindingResult binding,
            @ModelAttribute("currentUser") User currentUser) {
        if (!binding.hasErrors())
            issueService.update(issueId, issueRequest, currentUser);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/issues";
    }

    @PatchMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}/status")
    public String setStatus(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable UUID issueId,
            @RequestParam String status,
            @ModelAttribute("currentUser") User currentUser) {
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        issueService.setStatus(issueId, status, currentUser, translation.getId());
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/issues";
    }

    @DeleteMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}")
    public String delete(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable UUID issueId,
            @ModelAttribute("currentUser") User currentUser) {
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        issueService.delete(issueId, currentUser, translation.getId());
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/issues";
    }
}
