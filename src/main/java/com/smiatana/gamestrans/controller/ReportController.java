package com.smiatana.gamestrans.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.smiatana.gamestrans.dto.ReportRequest;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.ModerationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReportController {
    private final ModerationService moderationService;
    private final AuthService authService;

    @PostMapping("/reports")
    @ResponseBody
    public ResponseEntity<String> report(
            @RequestBody ReportRequest req) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null)
            return ResponseEntity.status(401).body("Трэба ўвайсці");
        try {
            moderationService.submitReport(currentUser, req.getTargetType(), req.getTargetId(), req.getReason());
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}