package com.smiatana.gamestrans.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smiatana.gamestrans.service.FileStorageService;
import com.smiatana.gamestrans.service.AuthService;
import com.smiatana.gamestrans.service.AuditLogService;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
public class ImageUploadController {
    private final FileStorageService fileStorageService;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @PostMapping("/api/upload/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam MultipartFile file) throws IOException {
        String url = fileStorageService.store(file, "content");
        var currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            auditLogService.log(currentUser, "IMAGE_UPLOAD", "upload", null,
                    "Файл загружаны карыстальнікам " + currentUser.getUsername() + ": " + url);
        }
        return ResponseEntity.ok(Map.of("url", url));
    }

}
