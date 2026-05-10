package com.smiatana.gamestrans.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.smiatana.gamestrans.dto.CommentRequest;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.CommentService;
import com.smiatana.gamestrans.service.UriService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final TranslationRepository translationRepository;
    private final AuthService authService;
    private final UriService uriService;

    @PostMapping("/g/{gameTitle}/t/{transTitle}/comments")
    public String create(@PathVariable String gameTitle, @PathVariable String transTitle,
            @Valid @ModelAttribute CommentRequest commentRequest, BindingResult binding) {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        if (!binding.hasErrors())
            commentService.create(commentRequest, currentUser, translation.getId());
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);

        return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;
    }

    @PatchMapping("/g/{gameTitle}/t/{transTitle}/comments/{commentId}")
    public String update(@PathVariable String gameTitle, @PathVariable String transTitle, @PathVariable UUID commentId,
            @Valid @ModelAttribute CommentRequest commentRequest, BindingResult binding) {
        User currentUser = authService.getCurrentUser();
        if (!binding.hasErrors())
            commentService.update(commentId, commentRequest, currentUser);
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);

        return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;
    }

    @DeleteMapping("/g/{gameTitle}/t/{transTitle}/comments/{commentId}")
    public String delete(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable UUID commentId) {
        User currentUser = authService.getCurrentUser();
        Translation translation = translationRepository
                .findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        commentService.delete(commentId, currentUser, translation.getId());
        String uriGameTitle = uriService.uri(gameTitle);
        String uriTransTitle = uriService.uri(transTitle);

        return "redirect:/g/" + uriGameTitle + "/t/" + uriTransTitle;
    }
}
