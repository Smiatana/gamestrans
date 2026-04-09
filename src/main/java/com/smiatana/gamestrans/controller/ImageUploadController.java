package com.smiatana.gamestrans.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smiatana.gamestrans.service.FileStorageService;

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

    @PostMapping("/api/upload/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam MultipartFile file) throws IOException {
        String url = fileStorageService.store(file, "content");
        return ResponseEntity.ok(Map.of("url", url));
    }

}
