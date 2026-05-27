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
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.IssueService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class IssueController {
        private final IssueService issueService;
        private final IssueRepository issueRepository;
        private final TranslationRepository translationRepository;
        private final TranslationMemberRepository translationMemberRepository;
        private final AuthService authService;
        private final UriService uriService;

        @GetMapping("/g/{gameTitle}/t/{transTitle}/issues")
        public String index(@PathVariable String gameTitle, @PathVariable String transTitle,
                        @RequestParam(name = "edit", required = false) UUID editIssueId,
                        Model model) {
                return renderIssuePage(gameTitle, transTitle, model, new IssueRequest(), editIssueId, null);
        }

        @PostMapping("/g/{gameTitle}/t/{transTitle}/issues")
        public String create(@PathVariable String gameTitle, @PathVariable String transTitle,
                        @Valid @ModelAttribute IssueRequest issueRequest,
                        BindingResult binding, Model model) {
                if (binding.hasErrors()) {
                        return renderIssuePage(gameTitle, transTitle, model, issueRequest, null, null);
                }
                User currentUser = authService.getCurrentUser();
                Translation translation = translationRepository
                                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
                issueService.create(issueRequest, currentUser, translation.getId());
                String uriGameTitle = uriService.uri(gameTitle);
                String uriTransTitle = uriService.uri(transTitle);

                return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle + "/issues";
        }

        @GetMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}/edit")
        public String editPage(@PathVariable String gameTitle, @PathVariable String transTitle,
                        @PathVariable UUID issueId, Model model) {
                User currentUser = authService.getCurrentUser();
                Issue issue = issueRepository.findById(issueId).orElseThrow();
                if (currentUser == null || !issue.getAuthor().getId().equals(currentUser.getId()))
                        return "redirect:/g/" + uriService.uri(gameTitle) + "/t/" + uriService.uri(transTitle) + "/issues";

                String uriGameTitle = uriService.uri(gameTitle);
                String uriTransTitle = uriService.uri(transTitle);
                return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle + "/issues?edit=" + issueId;
        }

        @PatchMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}")
        public String update(@PathVariable String gameTitle, @PathVariable String transTitle,
                        @PathVariable UUID issueId,
                        @Valid @ModelAttribute IssueRequest issueRequest,
                        BindingResult binding, Model model) {
                if (binding.hasErrors()) {
                        return renderIssuePage(gameTitle, transTitle, model, new IssueRequest(), issueId, issueRequest);
                }
                User currentUser = authService.getCurrentUser();
                issueService.update(issueId, issueRequest, currentUser);
                String uriGameTitle = uriService.uri(gameTitle);
                String uriTransTitle = uriService.uri(transTitle);

                return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle + "/issues";
        }

        private String renderIssuePage(String gameTitle, String transTitle, Model model,
                        IssueRequest issueRequest, UUID editIssueId, IssueRequest editIssueRequest) {
                User currentUser = authService.getCurrentUser();
                Translation translation = translationRepository
                                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
                List<Issue> issues = issueRepository
                                .findByTranslationIdOrderByCreatedAtDesc(translation.getId());
                boolean isMember = currentUser != null && translationMemberRepository
                                .existsByTranslationIdAndUserEmail(translation.getId(), currentUser.getEmail());

                model.addAttribute("translation", translation);
                model.addAttribute("issues", issues);
                model.addAttribute("isMember", isMember);
                model.addAttribute("issueRequest", issueRequest != null ? issueRequest : new IssueRequest());
                model.addAttribute("editIssueId", editIssueId);

                if (editIssueId != null && currentUser != null) {
                        if (editIssueRequest != null) {
                                model.addAttribute("editIssueRequest", editIssueRequest);
                        } else {
                                Issue issue = issueRepository.findById(editIssueId).orElse(null);
                                if (issue != null && issue.getAuthor().getId().equals(currentUser.getId())) {
                                        IssueRequest request = new IssueRequest();
                                        request.setTitle(issue.getTitle());
                                        request.setDescription(issue.getDescription());
                                        request.setReleaseTitle(issue.getReleaseTitle());
                                        model.addAttribute("editIssueRequest", request);
                                }
                        }
                }
                return "issues/index";
        }

        @PatchMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}/status")
        public String setStatus(@PathVariable String gameTitle, @PathVariable String transTitle,
                        @PathVariable UUID issueId,
                        @RequestParam String status) {
                User currentUser = authService.getCurrentUser();
                Translation translation = translationRepository
                                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
                issueService.setStatus(issueId, status, currentUser, translation.getId());

                String uriGameTitle = uriService.uri(gameTitle);
                String uriTransTitle = uriService.uri(transTitle);

                return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle + "/issues";
        }

        @DeleteMapping("/g/{gameTitle}/t/{transTitle}/issues/{issueId}")
        public String delete(@PathVariable String gameTitle, @PathVariable String transTitle,
                        @PathVariable UUID issueId) {
                User currentUser = authService.getCurrentUser();
                Translation translation = translationRepository
                                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
                issueService.delete(issueId, currentUser, translation.getId());
                String uriGameTitle = uriService.uri(gameTitle);
                String uriTransTitle = uriService.uri(transTitle);

                return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle + "/issues";
        }
}
