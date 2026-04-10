package com.smiatana.gamestrans.controller;

import com.smiatana.gamestrans.service.UserService;
import java.io.IOException;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

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
    public String show(@PathVariable String username, @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = userRepository.findByUsername(username).orElseThrow();
        if (user == null) {
            return "redirect:/";
        }
        model.addAttribute(user);

        List<Game> games = translationMemberRepository.findByUserEmail(user.getEmail()).stream()
                .map(tm -> tm.getTranslation().getGame()).distinct().toList();

        model.addAttribute("games", games);

        boolean isOwner = userDetails != null &&
                user.getEmail().equals(userDetails.getUsername());
        model.addAttribute("isOwner", isOwner);
        return "users/show";
    }

    @GetMapping("/u/{username}/edit")
    public String edit(@PathVariable String username, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(username).orElseThrow();
        boolean isOwner = userDetails != null &&
                user.getEmail().equals(userDetails.getUsername());
        if (!isOwner) {
            return "redirect:/u/" + username;
        }

        EditProfileRequest req = new EditProfileRequest();
        req.setUsername(user.getUsername());
        req.setBio(user.getBio());

        model.addAttribute("user", user);
        model.addAttribute("editProfileRequest", req);
        return "users/edit";
    }

    @PostMapping("/u/{username}/edit")
    public String edit(@PathVariable String username, @Valid @ModelAttribute EditProfileRequest editProfileRequest,
            BindingResult binding, @AuthenticationPrincipal UserDetails userDetails, Model model) throws IOException {

        if (binding.hasErrors())
            return "/u/" + username + "/edit";

        User user = userRepository.findByUsername(username).orElseThrow();
        userService.update(user.getId(), editProfileRequest);

        String uriUsername = uriService.uri(editProfileRequest.getUsername());
        return "redirect:/u/" + uriUsername;
    }

}
