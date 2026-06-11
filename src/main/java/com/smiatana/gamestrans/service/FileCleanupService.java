package com.smiatana.gamestrans.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.UploadedFile;
import com.smiatana.gamestrans.repository.GameRepository;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UploadedFileRepository;
import com.smiatana.gamestrans.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileCleanupService {
    private static final Pattern LOCAL_UPLOAD_URL = Pattern.compile("(?:src|href)\\s*=\\s*['\"]([^'\"]+)['\"]");

    private final UploadedFileRepository uploadedFileRepository;
    private final TranslationRepository translationRepository;
    private final ReleaseRepository releaseRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanup() {
        List<String> allPaths = uploadedFileRepository.findAll()
                .stream().map(UploadedFile::getPath).toList();

        Set<String> inUse = new HashSet<>();

        userRepository.findAll().forEach(u -> {
            if (u.getAvatarUrl() != null) {
                inUse.add(u.getAvatarUrl());
            }
            if (u.getBio() != null)
                extractUrls(u.getBio(), inUse);
        });
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
            if (g.getBackgroundUrl() != null) {
                inUse.add(g.getBackgroundUrl());
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
        Matcher matcher = LOCAL_UPLOAD_URL.matcher(html);
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url != null && url.startsWith("/uploads/")) {
                urls.add(url);
            }
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
