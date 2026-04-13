package com.smiatana.gamestrans.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.MemberRequestService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberRequestController {
    private final MemberRequestService memberRequestService;
    private final TranslationRepository translationRepository;

    @PostMapping("/g/{gameTitle}/{transTitle}/members/invite/{username}")
    public String invite(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        memberRequestService.sendInvite(translation.getId(), userDetails.getUsername(), username);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/edit";
    }

    @PostMapping("/g/{gameTitle}/{transTitle}/members/kick/{username}")
    public String kick(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        memberRequestService.kick(translation.getId(), userDetails.getUsername(), username);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/edit";
    }
}