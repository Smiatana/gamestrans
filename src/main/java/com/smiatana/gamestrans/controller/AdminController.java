package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.smiatana.gamestrans.entity.AppSettings;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.AppSettingsRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.AuditLogService;
import com.smiatana.gamestrans.service.FileStorageService;
import com.smiatana.gamestrans.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final AppSettingsRepository appSettingsRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    private boolean isAdmin(User u) {
        return u != null && "admin".equals(u.getRole());
    }

    @GetMapping("/moderators")
    public String moderators(@ModelAttribute("currentUser") User currentUser, Model model) {
        if (!isAdmin(currentUser))
            return "redirect:/";
        List<User> moderators = userRepository.findByRoleOrderByCreatedAtDesc("moderator");
        model.addAttribute("moderators", moderators);
        return "admin/moderators";
    }

    @PostMapping("/moderators/promote/{id}")
    public String promote(@PathVariable UUID id, @ModelAttribute("currentUser") User currentUser) {
        if (!isAdmin(currentUser))
            return "redirect:/";
        userService.setRole(id, "moderator");
        auditLogService.log(currentUser, "USER_PROMOTE", "user", id, "User promoted to moderator");
        return "redirect:/admin/moderators";
    }

    @PostMapping("/moderators/demote/{id}")
    public String demote(@PathVariable UUID id, @ModelAttribute("currentUser") User currentUser) {
        if (!isAdmin(currentUser))
            return "redirect:/";
        userService.setRole(id, "user");
        auditLogService.log(currentUser, "USER_DEMOTE", "user", id, "Moderator demoted to user");
        return "redirect:/admin/moderators";
    }

    @GetMapping("/settings")
    public String settingsPage(@ModelAttribute("currentUser") User currentUser, Model model) {
        if (!isAdmin(currentUser))
            return "redirect:/";
        AppSettings settings = appSettingsRepository.findById(1)
                .orElseGet(() -> {
                    AppSettings s = new AppSettings();
                    return appSettingsRepository.save(s);
                });
        model.addAttribute("settings", settings);
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
            @ModelAttribute("currentUser") User currentUser,
            @RequestParam(required = false) String footerContent,
            @RequestParam(required = false) String metaTags,
            @RequestParam(required = false) MultipartFile logoFile) throws java.io.IOException {
        if (!isAdmin(currentUser))
            return "redirect:/";

        AppSettings settings = appSettingsRepository.findById(1)
                .orElseGet(AppSettings::new);
        settings.setId(1);
        settings.setFooterContent(footerContent);
        settings.setMetaTags(metaTags);

        if (logoFile != null && !logoFile.isEmpty()) {
            String url = fileStorageService.store(logoFile, "settings");
            settings.setLogoUrl(url);
        }
        appSettingsRepository.save(settings);
        auditLogService.log(currentUser, "SETTINGS_UPDATE", "settings", null, "App settings updated");
        return "redirect:/admin/settings?saved";
    }
}