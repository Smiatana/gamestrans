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
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final UriService uriService;

    @GetMapping("/u/{username}")
    public String show(@PathVariable String username, @ModelAttribute("currentUser") User currentUser,
            Model model) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (user == null) {
            return "/users/notfound";
        }
        boolean isOwner = currentUser != null &&
                user.getEmail().equals(currentUser.getEmail());
        model.addAttribute("isOwner", isOwner);
        if (user.getStatus().equals("frozen")) {

            return "/users/notfound";
        }

        model.addAttribute(user);

        List<Game> games = translationMemberRepository.findByUserEmail(user.getEmail()).stream()
                .map(tm -> tm.getTranslation().getGame()).distinct().toList();

        model.addAttribute("games", games);
        return "users/show";
    }

    @GetMapping("/u/{username}/edit")
    public String edit(@PathVariable String username, @ModelAttribute("currentUser") User currentUser, Model model) {
        User user = userRepository.findByUsername(username).orElseThrow();
        boolean isOwner = currentUser != null &&
                user.getEmail().equals(currentUser.getEmail());
        if (!isOwner) {
            return "redirect:/u/" + username;
        }

        EditProfileRequest req = new EditProfileRequest();
        req.setUsername(user.getUsername());
        req.setBio(user.getBio());

        ChangePasswordRequest pwdReq = new ChangePasswordRequest();

        model.addAttribute("user", user);
        model.addAttribute("editProfileRequest", req);
        model.addAttribute("changePasswordRequest", pwdReq);
        return "users/edit";
    }

    @PatchMapping("/u/{username}/edit")
    public String edit(@PathVariable String username, @Valid @ModelAttribute EditProfileRequest editProfileRequest,
            BindingResult binding, @ModelAttribute("currentUser") User currentUser, Model model) throws IOException {

        if (binding.hasErrors())
            return "/u/" + username + "/edit";

        User user = userRepository.findByUsername(username).orElseThrow();

        if (currentUser == null || !user.getEmail().equals(currentUser.getEmail())) {
            return "redirect:/u/" + username;
        }
        userService.update(user.getId(), editProfileRequest);

        String uriUsername = uriService.uri(editProfileRequest.getUsername());
        return "redirect:/u/" + uriUsername;
    }

    @PatchMapping("/u/{username}/changepassword")
    public String changepassword(@PathVariable String username,
            @Valid @ModelAttribute ChangePasswordRequest changePasswordRequest,
            BindingResult binding, @ModelAttribute("currentUser") User currentUser, Model model) throws IOException {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (currentUser == null || !user.getEmail().equals(currentUser.getEmail())) {
            return "redirect:/u/" + username;
        }
        if (binding.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("editProfileRequest", new EditProfileRequest());
            return "users/edit";
        }
        try {
            userService.changePassword(user.getId(), changePasswordRequest);
        } catch (IllegalArgumentException e) {
            EditProfileRequest req = new EditProfileRequest();
            req.setUsername(user.getUsername());
            req.setBio(user.getBio());
            model.addAttribute("user", user);
            model.addAttribute("editProfileRequest", req);
            model.addAttribute("changePasswordRequest", changePasswordRequest);
            model.addAttribute("passwordError", e.getMessage());
            return "users/edit";
        }
        return "redirect:/u/" + username;
    }

    @DeleteMapping("/u/{username}/delete")
    public String deleteUser(@PathVariable String username,
            @ModelAttribute("currentUser") User currentUser) {
        User user = userRepository.findByUsername(username).orElseThrow();
        boolean isOwner = currentUser != null &&
                user.getEmail().equals(currentUser.getEmail());
        if (!isOwner)
            return "redirect:/u/" + username;
        userService.delete(user.getId());
        return "redirect:/";
    }

    @GetMapping("/users/search")
    @ResponseBody
    public List<Map<String, String>> search(@RequestParam String q) {
        return userRepository.findByUsernameContainingIgnoreCase(q)
                .stream()
                .map(u -> Map.of(
                        "username", u.getUsername(),
                        "avatarUrl", u.getAvatarUrl() != null ? u.getAvatarUrl() : ""))
                .toList();
    }

}
