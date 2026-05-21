package com.smiatana.gamestrans.controller;

import com.smiatana.gamestrans.service.UserService;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.smiatana.gamestrans.dto.ChangePasswordRequest;
import com.smiatana.gamestrans.dto.EditProfileRequest;
import com.smiatana.gamestrans.entity.Game;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.entity.Subscription;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.repository.WarningRepository;
import com.smiatana.gamestrans.service.AuditLogService;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.SubscriptionService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final GameRepository gameRepository;
    private final TranslationRepository translationRepository;
    private final WarningRepository warningRepository;
    private final AuditLogService auditLogService;
    private final UriService uriService;
    private final AuthService authService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/u/{username}")
    public String show(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username).orElseThrow();

        if (user.isDeleted())
            return "users/notfound";
        User currentUser = authService.getCurrentUser();
        if ("frozen".equals(user.getStatus())) {
            boolean isOwner = currentUser != null && user.getEmail().equals(currentUser.getEmail());
            if (!isOwner)
                return "users/notfound";
        }

        boolean isOwner = currentUser != null && user.getEmail().equals(currentUser.getEmail());
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("user", user);

        List<Game> games = translationMemberRepository.findByUserEmail(user.getEmail()).stream()
                .map(tm -> tm.getTranslation().getGame())
                .filter(g -> g.getDeletedAt() == null)
                .distinct().toList();
        model.addAttribute("games", games);

        List<Game> deletedGames = List.of();
        if (isOwner) {
            deletedGames = translationMemberRepository.findByUserEmail(user.getEmail()).stream()
                    .map(tm -> tm.getTranslation().getGame())
                    .filter(g -> g.getDeletedAt() != null)
                    .distinct().toList();
        }
        model.addAttribute("deletedGames", deletedGames);

        long warningCount = isOwner ? warningRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size() : 0;
        model.addAttribute("warningCount", warningCount);

        boolean isSubscribedToUser = subscriptionService.isSubscribed(
        currentUser, SubscriptionService.TYPE_USER, user.getId());
        model.addAttribute("isSubscribedToUser", isSubscribedToUser);

        long subscriberCount = subscriptionService.countSubscribersOfUser(user.getId());
        model.addAttribute("subscriberCount", subscriberCount);
        
        if (isOwner) {
            List<Subscription> mySubscribers = subscriptionService.getSubscribersOfUser(user.getId());
            model.addAttribute("mySubscribers", mySubscribers);
        
            List<Subscription> gameSubscriptions = subscriptionService.getSubscriptionsOfType(user, SubscriptionService.TYPE_GAME);
            populateSubscriptionTitles(gameSubscriptions);
            model.addAttribute("gameSubscriptions", gameSubscriptions);

            List<Subscription> translationSubscriptions = subscriptionService.getSubscriptionsOfType(user, SubscriptionService.TYPE_TRANSLATION);
            populateSubscriptionTitles(translationSubscriptions);
            model.addAttribute("translationSubscriptions", translationSubscriptions);

            List<Subscription> userSubscriptions = subscriptionService.getSubscriptionsOfType(user, SubscriptionService.TYPE_USER);
            populateSubscriptionTitles(userSubscriptions);
            model.addAttribute("userSubscriptions", userSubscriptions);
        }



        return "users/show";
    }

    private void populateSubscriptionTitles(List<Subscription> subscriptions) {
        for (Subscription sub : subscriptions) {
            if (SubscriptionService.TYPE_GAME.equals(sub.getTargetType())) {
                gameRepository.findById(sub.getTargetId()).ifPresent(game -> {
                    sub.setResolvedTitle(game.getTitle());
                });
            } else if (SubscriptionService.TYPE_TRANSLATION.equals(sub.getTargetType())) {
                translationRepository.findById(sub.getTargetId()).ifPresent(translation -> {
                    sub.setResolvedTitle(translation.getTitle());
                    sub.setResolvedUrl("/g/" + translation.getGame().getTitle() + "/t/" + translation.getTitle());
                });
            } else if (SubscriptionService.TYPE_USER.equals(sub.getTargetType())) {
                userRepository.findById(sub.getTargetId()).ifPresent(subscribedUser -> {
                    sub.setResolvedTitle(subscribedUser.getUsername());
                });
            }
        }
    }

    @GetMapping("/u/{username}/edit")
    public String edit(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username).orElseThrow();
        User currentUser = authService.getCurrentUser();
        if (currentUser == null || !user.getEmail().equals(currentUser.getEmail()))
            return "redirect:/u/" + username;
        EditProfileRequest editProfileRequest = new EditProfileRequest();
        editProfileRequest.setUsername(user.getUsername());
        editProfileRequest.setBio(user.getBio());
        model.addAttribute("user", user);
        model.addAttribute("editProfileRequest", editProfileRequest);
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "users/edit";
    }

    @PatchMapping("/u/{username}/edit")
    public String edit(@PathVariable String username,
            @Valid @ModelAttribute EditProfileRequest editProfileRequest,
            BindingResult binding, Model model) throws IOException {

        User currentUser = authService.getCurrentUser();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        if (currentUser == null || !user.getEmail().equals(currentUser.getEmail()))
            return "redirect:/u/" + username;
        
        if (binding.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
            return "users/edit";
        }

        userService.update(user.getId(), editProfileRequest);
        auditLogService.log(currentUser, "USER_UPDATE", "user", user.getId(),
            "Профіль абноўлены: " + user.getUsername());
        return "redirect:/u/" + uriService.uri(editProfileRequest.getUsername());
    }

    @PatchMapping("/u/{username}/changepassword")
    public String changePassword(@PathVariable String username,
            @Valid @ModelAttribute ChangePasswordRequest changePasswordRequest,
            BindingResult binding, Model model) throws IOException {
        
        User currentUser = authService.getCurrentUser();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (currentUser == null || !user.getEmail().equals(currentUser.getEmail()))
            return "redirect:/u/" + username;
        
        EditProfileRequest editProfileRequest = new EditProfileRequest();
        editProfileRequest.setUsername(user.getUsername());
        editProfileRequest.setBio(user.getBio());
        
        if (binding.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("editProfileRequest", editProfileRequest);
            model.addAttribute("changePasswordRequest", changePasswordRequest);
            return "users/edit";
        }
        
        try {
            userService.changePassword(user.getId(), changePasswordRequest);
            auditLogService.log(currentUser, "USER_PASSWORD_CHANGE", "user", user.getId(),
                    "Пароль абноўлены карыстальнікам " + user.getUsername());
        } catch (IllegalArgumentException e) {
            model.addAttribute("user", user);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("editProfileRequest", editProfileRequest);
            model.addAttribute("changePasswordRequest", changePasswordRequest);
            model.addAttribute("passwordError", e.getMessage());
            return "users/edit";
        }
        return "redirect:/u/" + username;
    }

    @DeleteMapping("/u/{username}/delete")
    public String deleteUser(@PathVariable String username,
            HttpServletRequest request) {
        User currentUser = authService.getCurrentUser();
        User user = userRepository.findByUsername(username).orElseThrow();
        if (currentUser == null || !user.getEmail().equals(currentUser.getEmail()))
            return "redirect:/u/" + username;

        userService.softDelete(user.getId());
        auditLogService.log(currentUser, "USER_DELETE", "user", user.getId(),
                "User deleted own account: " + user.getUsername());

        request.getSession().invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @PostMapping("/g/{gameTitle}/restore")
    public String restoreGame(@PathVariable String gameTitle) {
        User currentUser = authService.getCurrentUser();
        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        boolean isOwner = translationMemberRepository
                .findByUserEmail(currentUser.getEmail()).stream()
                .anyMatch(tm -> tm.getTranslation().getGame().getId().equals(game.getId()));
        if (!isOwner)
            return "redirect:/";

        game.setDeletedAt(null);
        gameRepository.save(game);
        auditLogService.log(currentUser, "GAME_RESTORE", "game", game.getId(),
            "Гульня '" + game.getTitle() + "' адноўлена карыстальнікам " + currentUser.getUsername());
        return "redirect:/u/" + currentUser.getUsername();
    }

    @PostMapping("/g/{gameTitle}/delete-permanent")
    public String deletePermanent(@PathVariable String gameTitle) {
        User currentUser = authService.getCurrentUser();
        Game game = gameRepository.findByTitle(gameTitle).orElseThrow();
        boolean isOwner = translationMemberRepository
                .findByUserEmail(currentUser.getEmail()).stream()
                .anyMatch(tm -> tm.getTranslation().getGame().getId().equals(game.getId()));
        if (!isOwner)
            return "redirect:/";

        if (game.getDeletedAt() == null)
            return "redirect:/u/" + currentUser.getUsername();

        gameRepository.deleteById(game.getId());
        auditLogService.log(currentUser, "GAME_DELETE_PERMANENT", "game", game.getId(),
            "Гульня '" + game.getTitle() + "' назаўсёды выдалена");
        return "redirect:/u/" + currentUser.getUsername();
    }

    @GetMapping("/users/search")
    @ResponseBody
    public List<Map<String, String>> search(@RequestParam String q) {
        return userRepository.findByUsernameContainingIgnoreCase(q).stream()
                .filter(u -> !u.isDeleted())
                .map(u -> Map.of(
                        "id", u.getId().toString(),
                        "username", u.getUsername(),
                        "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : ""))
                .toList();
    }
}