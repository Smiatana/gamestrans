package com.smiatana.gamestrans.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smiatana.gamestrans.dto.BanRequest;
import com.smiatana.gamestrans.entity.*;
import com.smiatana.gamestrans.repository.*;
import com.smiatana.gamestrans.service.*;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/mod")
@RequiredArgsConstructor
public class ModerationController {

    private final TranslationService translationService;
    private final GameService gameService;
    private final ReleaseRepository releaseRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final AuditLogRepository auditLogRepository;
    private final CommentRepository commentRepository;
    private final GameRepository gameRepository;
    private final TranslationRepository translationRepository;
    private final IssueRepository issueRepository;
    private final ModerationService moderationService;
    private final ReleaseService releaseService;
    private final AuditLogService auditLogService;
    private final AuthService authService;

    private boolean isMod(User u) {
        return u != null && (u.getRole().equals("moderator") || u.getRole().equals("admin"));
    }


    @GetMapping("")
    public String dashboard(Model model) {
        User currentUser = authService.getCurrentUser();
        System.out.println("this user's ROLE: " + currentUser.getRole());
        if (!isMod(currentUser))
            return "redirect:/";

        model.addAttribute("pendingReleaseCount",
                releaseRepository.findByStatusOrderByCreatedAtAsc("on_review").size());
        model.addAttribute("pendingReportCount",
                complaintRepository.findByStatusOrderByCreatedAtDesc("pending", PageRequest.of(0, 1))
                        .getTotalElements());

        return "moderation/dashboard";
    }

    @GetMapping("/releases")
    public String releases(
            @RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").ascending());
        Page<Release> pending = releaseRepository.findByStatusOrderByCreatedAtDesc("on_review", pageable);

        model.addAttribute("releases", pending.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pending.getTotalPages());
        return "moderation/releases";
    }

    @PostMapping("/releases/{id}/approve")
    public String approveRelease(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        releaseService.approve(id, currentUser);
        return "redirect:/mod/releases";
    }

    @PostMapping("/releases/{id}/reject")
    public String rejectRelease(@PathVariable UUID id,
            @RequestParam(required = false) String note) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        releaseService.reject(id, currentUser, note);
        return "redirect:/mod/releases";
    }

    @PostMapping("/releases/{id}/delete")
    public String deleteRelease(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        releaseService.softDelete(id, currentUser);
        return "redirect:/mod/releases";
    }

    @GetMapping("/users")
    public String users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q,
            Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";

        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAllByOrderByCreatedAtDesc(pageable);

        model.addAttribute("users", users.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        return "moderation/users";
    }

    @PostMapping("/users/{id}/ban")
    public String banUser(@PathVariable UUID id,
            @ModelAttribute BanRequest req) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.banUser(id, currentUser, req.getDurationDays(), req.getReason(), req.getNote());
        return "redirect:/mod/users";
    }

    @PostMapping("/users/{id}/unban")
    public String unbanUser(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.unbanUser(id, currentUser);
        return "redirect:/mod/users";
    }

    @PostMapping("/users/{id}/warn")
    public String warnUser(@PathVariable UUID id,
            @RequestParam String reason) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.warnUser(id, currentUser, reason);
        return "redirect:/mod/users";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());
        Page<Complaint> reports = "all".equals(status)
                ? complaintRepository.findAllByOrderByCreatedAtDesc(pageable)
                : complaintRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        model.addAttribute("reports", reports.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reports.getTotalPages());
        model.addAttribute("selectedStatus", status);
        return "moderation/reports";
    }

    @PostMapping("/reports/{id}/approve")
    public String approveReport(@PathVariable UUID id,
            @RequestParam String reason) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.resolveReport(id, currentUser, "approved", reason);
        return "redirect:/mod/reports";
    }

    @PostMapping("/reports/{id}/decline")
    public String declineReport(@PathVariable UUID id,
            @RequestParam String reason) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.resolveReport(id, currentUser, "declined", reason);
        return "redirect:/mod/reports";
    }

    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.deleteReport(id, currentUser);
        return "redirect:/mod/reports";
    }

    @GetMapping("/content/games")
    public String contentGames(@RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        model.addAttribute("games", gameRepository.findAll(pageable).getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", gameRepository.findAll(pageable).getTotalPages());
        return "moderation/content-games";
    }

    @GetMapping("/content/translations")
    public String contentTranslations(@RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        var translationPage = translationRepository.findAll(pageable);
        model.addAttribute("translations", translationPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", translationPage.getTotalPages());
        return "moderation/content-translations";
    }

    @GetMapping("/content/releases")
    public String contentReleases(@RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        var releasePage = releaseRepository.findAll(pageable);
        model.addAttribute("releases", releasePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", releasePage.getTotalPages());
        return "moderation/content-releases";
    }

    @GetMapping("/content/comments")
    public String contentComments(@RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        var commentPage = commentRepository.findAll(pageable);
        model.addAttribute("comments", commentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", commentPage.getTotalPages());
        return "moderation/content-comments";
    }

    @GetMapping("/content/issues")
    public String contentIssues(@RequestParam(defaultValue = "0") int page, Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Pageable pageable = PageRequest.of(page, 30, Sort.by("createdAt").descending());
        var issuePage = issueRepository.findAll(pageable);
        model.addAttribute("issues", issuePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", issuePage.getTotalPages());
        return "moderation/content-issues";
    }

    @PostMapping("/content/games/{id}/hide")
    public String hideGame(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Game game = gameRepository.findById(id).orElseThrow();
        game.setDeletedAt(java.time.LocalDateTime.now());
        gameRepository.save(game);
        auditLogService.log(currentUser, "GAME_HIDE", "game", id, "Game hidden by mod");
        return "redirect:/mod/content/games";
    }

    @PostMapping("/content/comments/{id}/delete")
    public String deleteComment(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        commentRepository.deleteById(id);
        auditLogService.log(currentUser, "COMMENT_DELETE", "comment", id, "Comment deleted by mod");
        return "redirect:/mod/content/comments";
    }

    @PostMapping("/content/releases/{id}/delete")
    public String deleteContentRelease(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Release release = releaseRepository.findById(id).orElseThrow();
        release.setDeletedAt(java.time.LocalDateTime.now());
        releaseRepository.save(release);
        auditLogService.log(currentUser, "RELEASE_DELETE", "release", id, "Release deleted by mod");
        return "redirect:/mod/content/releases";
    }

    @PostMapping("/content/releases/{id}/restore")
    public String restoreContentRelease(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Release release = releaseRepository.findById(id).orElseThrow();
        release.setDeletedAt(null);
        releaseRepository.save(release);
        auditLogService.log(currentUser, "RELEASE_RESTORE", "release", id, "Release restored by mod");
        return "redirect:/mod/content/releases";
    }

    @PostMapping("/content/games/{id}/restore")
    public String restoreGame(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Game game = gameRepository.findById(id).orElseThrow();
        game.setDeletedAt(null);
        gameRepository.save(game);
        auditLogService.log(currentUser, "GAME_RESTORE", "game", id, "Game restored by mod");
        return "redirect:/mod/content/games";
    }

    @PostMapping("/content/translations/{id}/delete")
    public String deleteContentTranslation(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Translation translation = translationRepository.findById(id).orElseThrow();
        translation.setDeletedAt(java.time.LocalDateTime.now());
        translationRepository.save(translation);
        auditLogService.log(currentUser, "TRANSLATION_DELETE", "translation", id, "Translation deleted by mod");
        return "redirect:/mod/content/translations";
    }

    @PostMapping("/content/translations/{id}/restore")
    public String restoreTranslation(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        Translation translation = translationRepository.findById(id).orElseThrow();
        translation.setDeletedAt(null);
        translationRepository.save(translation);
        auditLogService.log(currentUser, "TRANSLATION_RESTORE", "translation", id, "Translation restored by mod");
        return "redirect:/mod/content/translations";
    }

    @PostMapping("/content/games/{id}/delete-permanent")
    public String deleteGamePermanently(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        gameService.delete(id);
        auditLogService.log(currentUser, "GAME_DELETE_PERMANENT", "game", id, "Game permanently deleted by mod");
        return "redirect:/mod/content/games";
    }

    @PostMapping("/content/translations/{id}/delete-permanent")
    public String deleteTranslationPermanently(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        translationService.delete(id);
        auditLogService.log(currentUser, "TRANSLATION_DELETE_PERMANENT", "translation", id, "Translation permanently deleted by mod");
        return "redirect:/mod/content/translations";
    }

    @PostMapping("/content/releases/{id}/delete-permanent")
    public String deleteReleasePermanently(@PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        releaseService.delete(id, currentUser);
        auditLogService.log(currentUser, "TRANSLATION_DELETE_PERMANENT", "translation", id, "Translation permanently deleted by mod");
        return "redirect:/mod/content/releases";
    }

    @GetMapping("/logs")
    public String logs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String type,
            Model model) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";

        Pageable pageable = PageRequest.of(page, 50, Sort.by("createdAt").descending());
        var logPage = (type != null && !type.isBlank())
                ? auditLogRepository.findByTargetTypeOrderByCreatedAtDesc(type, pageable)
                : auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("selectedType", type);
        return "moderation/logs";
    }

    @GetMapping("/api/item/{type}/{id}")
    @ResponseBody
    public java.util.Map<String, Object> getItemDetails(@PathVariable String type, @PathVariable UUID id) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            throw new RuntimeException("Unauthorized");

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        switch (type) {
            case "game":
                Game game = gameRepository.findById(id).orElseThrow();
                result.put("назва", game.getTitle());
                result.put("распрацоўшчык", game.getDeveloper());
                result.put("годВыпуску", game.getReleaseYear());
                result.put("жанры", game.getGenres().stream().map(g -> g.getName()).toList());
                result.put("апісанне", game.getDescription());
                result.put("створанаКім", game.getCreatedBy().getUsername());
                result.put("створанаКалі", game.getCreatedAt());
                result.put("спасылкаНаВокладку", game.getCoverUrl());
                result.put("спасылкаНаФон", game.getBackgroundUrl());
                result.put("выдалена", game.getDeletedAt() != null);
                break;
            case "translation":
                Translation translation = translationRepository.findById(id).orElseThrow();
                result.put("назва", translation.getTitle());
                result.put("гульня", translation.getGame().getTitle());
                result.put("апісанне", translation.getDescription());
                result.put("статус", translation.getStatus());
                result.put("створанаКім", translation.getCreatedBy().getUsername());
                result.put("створанаКалі", translation.getCreatedAt());
                result.put("выдалена", translation.getDeletedAt() != null);
                break;
            case "release":
                Release release = releaseRepository.findById(id).orElseThrow();
                result.put("назва", release.getTitle());
                result.put("пераклад", release.getTranslation().getTitle());
                result.put("гульня", release.getTranslation().getGame().getTitle());
                result.put("апісанне", release.getDescription());
                result.put("спасылкаНаФайл", release.getFileUrl());
                result.put("статус", release.getStatus());
                result.put("створанаКім", release.getCreatedBy().getUsername());
                result.put("створанаКалі", release.getCreatedAt());
                result.put("выдалена", release.getDeletedAt() != null);
                break;
            case "comment":
                Comment comment = commentRepository.findById(id).orElseThrow();
                result.put("змест", comment.getBody());
                result.put("пераклад", comment.getTranslation().getTitle());
                result.put("гульня", comment.getTranslation().getGame().getTitle());
                result.put("аўтар", comment.getAuthor().getUsername());
                result.put("створанаКалі", comment.getCreatedAt());
                break;
            case "issue":
                Issue issue = issueRepository.findById(id).orElseThrow();
                result.put("назва", issue.getTitle());
                result.put("апісанне", issue.getDescription());
                result.put("назваРэлізу", issue.getReleaseTitle());
                result.put("статус", issue.getStatus());
                result.put("пераклад", issue.getTranslation().getTitle());
                result.put("гульня", issue.getTranslation().getGame().getTitle());
                result.put("аўтар", issue.getAuthor().getUsername());
                result.put("створанаКалі", issue.getCreatedAt());
                break;
            case "report":
                Complaint complaint = complaintRepository.findById(id).orElseThrow();
                result.put("тыпСкаргі", complaint.getTargetType());
                result.put("ідэнтыфікатарСкаргі", complaint.getTargetId());
                result.put("прычына", complaint.getReason());
                result.put("статус", complaint.getStatus());
                result.put("нататкаМадэратара", complaint.getModeratorNote());
                result.put("аўтар", complaint.getAuthor().getUsername());
                result.put("створанаКалі", complaint.getCreatedAt());
                result.put("вырашанаКалі", complaint.getResolvedAt());
                break;
        }
        return result;
    }
}