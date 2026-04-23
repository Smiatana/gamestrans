package com.smiatana.gamestrans.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.service.MemberRequestService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberRequestController {
    private final MemberRequestService memberRequestService;
    private final TranslationRepository translationRepository;

    @PostMapping("/g/{gameTitle}/{transTitle}/members/invite/{username}")
    @ResponseBody
    public ResponseEntity<String> invite(@PathVariable String gameTitle,
            @PathVariable String transTitle,
            @PathVariable String username,
            @ModelAttribute("currentUser") User currentUser) {
        try {
            Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle)
                    .orElseThrow();

            memberRequestService.sendInvite(translation.getId(), currentUser.getEmail(), username);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/g/{gameTitle}/{transTitle}/members/kick/{username}")
    public String kick(@PathVariable String gameTitle, @PathVariable String transTitle,
            @PathVariable String username,
            @ModelAttribute("currentUser") User currentUser) {
        Translation translation = translationRepository.findByGameTitleAndTitle(gameTitle, transTitle).orElseThrow();
        memberRequestService.kick(translation.getId(), currentUser.getEmail(), username);
        return "redirect:/g/" + gameTitle + "/t/" + transTitle + "/edit";
    }
}