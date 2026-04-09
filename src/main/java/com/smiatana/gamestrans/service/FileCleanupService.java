package com.smiatana.gamestrans.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.UploadedFile;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UploadedFileRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileCleanupService {
    private final UploadedFileRepository uploadedFileRepository;
    private final TranslationRepository translationRepository;
    private final ReleaseRepository releaseRepository;
    private final GameRepository gameRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        List<String> allPaths = uploadedFileRepository.findAll()
                .stream().map(UploadedFile::getPath).toList();

        Set<String> inUse = new HashSet<>();

        translationRepository.findAll().forEach(t -> {
            if (t.getDescription() != null)
                extractUrls(t.getDescription(), inUse);
        });
        releaseRepository.findAll().forEach(r -> {
            if (r.getDescription() != null)
                extractUrls(r.getDescription(), inUse);
            if (r.getFileUrl() != null)
                inUse.add(r.getFileUrl());
        });

        gameRepository.findAll().forEach(g -> {
            if (g.getDescription() != null)
                extractUrls(g.getDescription(), inUse);
            if (g.getCoverUrl() != null) {
                inUse.add(g.getCoverUrl());
            }
        });

        for (String path : allPaths) {
            if (!inUse.contains(path)) {
                deleteFile(path);
                uploadedFileRepository.deleteByPath(path);
            }
        }
    }

    private void extractUrls(String html, Set<String> urls) {
        int i = 0;
        while ((i = html.indexOf("src=\"/uploads/", i)) != -1) {
            int start = i + 5;
            int end = html.indexOf("\"", start);
            if (end != -1)
                urls.add(html.substring(start, end));
            i = end;
        }
    }

    private void deleteFile(String urlPath) {
        try {
            Path file = Paths.get(urlPath.substring(1));
            Files.deleteIfExists(file);
        } catch (IOException e) {
        }
    }
}
