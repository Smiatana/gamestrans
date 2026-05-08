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

    @GetMapping
    public String dashboard(Model model) {
        User currentUser = authService.getCurrentUser();
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
            @RequestParam(required = false) String note) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.resolveReport(id, currentUser, "approved", note);
        return "redirect:/mod/reports";
    }

    @PostMapping("/reports/{id}/decline")
    public String declineReport(@PathVariable UUID id,
            @RequestParam(required = false) String note) {
        User currentUser = authService.getCurrentUser();
        if (!isMod(currentUser))
            return "redirect:/";
        moderationService.resolveReport(id, currentUser, "declined", note);
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
}