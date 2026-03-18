package com.smiatana.gamestrans.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import com.smiatana.gamestrans.dto.CreateTranslationRequest;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.service.TranslationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class TranslationController {
    private final TranslationService translationService;
    private final UserRepository userRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;

    @GetMapping("/translations/new")
    public String newTranslationPage(Model model) {
        model.addAttribute("createTranslationRequest", new CreateTranslationRequest());
        return "translations/new";
    }

    @PostMapping("/translations/new")
    public String createTranslation(
            @Valid @ModelAttribute CreateTranslationRequest createTranslationRequest,
            BindingResult binding,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) throws java.io.IOException {
        if (binding.hasErrors())
            return "translations/new";

        User currentUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        var translation = translationService.create(createTranslationRequest, currentUser);
        return "redirect:/translations/" + translation.getId();
    }

    @GetMapping("/translations/{id}")
    public String translationPage(@PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        Translation translation = translationRepository.findById(id).orElseThrow();
        List<TranslationMember> members = translationMemberRepository.findByTranslationId(id);

        boolean isOwner = members.stream()
                .anyMatch(m -> m.getUser().getEmail().equals(userDetails.getUsername())
                        && m.getRole().equals("owner"));

        model.addAttribute("translation", translation);
        model.addAttribute("members", members);
        model.addAttribute("isOwner", isOwner);
        return "translations/show";
    }

}
