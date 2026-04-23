package com.smiatana.gamestrans.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.CreateReleaseRequest;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.ReleaseRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final FileStorageService fileStorageService;
    private final ReleaseRepository releaseRepository;
    private final TranslationRepository translationRepository;

    @Transactional
    public Release create(CreateReleaseRequest req, User currentUser, Translation translation) throws IOException {
        if ((req.getReleaseFile() == null || req.getReleaseFile().isEmpty())
                && (req.getReleaseLink() == null || req.getReleaseLink().isBlank())) {
            throw new IllegalArgumentException("Рэліз не можа быць пустым");
        }

        String fileUrl;
        if (req.getReleaseFile() != null && !req.getReleaseFile().isEmpty()) {
            fileUrl = fileStorageService.store(req.getReleaseFile(), "releases");
        } else {
            fileUrl = req.getReleaseLink();
        }

        String status = normalizeReleaseStatus(req.getStatus());

        Release release = new Release();
        release.setTitle(req.getTitle());
        release.setFileUrl(fileUrl);
        release.setDescription(req.getDescription());
        release.setTranslation(translation);
        release.setCreatedBy(currentUser);
        release.setStatus(status);

        Release saved = releaseRepository.save(release);
        syncTranslationVisibility(translation.getId());
        return saved;
    }

    @Transactional
    public Release update(UUID releaseId, CreateReleaseRequest req) throws IOException {
        if ((req.getReleaseFile() == null || req.getReleaseFile().isEmpty())
                && (req.getReleaseLink() == null || req.getReleaseLink().isBlank())) {
            throw new IllegalArgumentException("Рэліз не можа быць пустым");
        }
        String fileUrl;
        if (req.getReleaseFile() != null && !req.getReleaseFile().isEmpty()) {
            fileUrl = fileStorageService.store(req.getReleaseFile(), "releases");
        } else {
            fileUrl = req.getReleaseLink();
        }

        String status = normalizeReleaseStatus(req.getStatus());

        Release release = releaseRepository.findById(releaseId).orElseThrow();
        release.setTitle(req.getTitle());
        release.setFileUrl(fileUrl);
        release.setDescription(req.getDescription());
        release.setStatus(status);
        Release saved = releaseRepository.save(release);
        syncTranslationVisibility(release.getTranslation().getId());
        return saved;
    }

    private void syncTranslationVisibility(UUID translationId) {
        Translation translation = translationRepository.findById(translationId).orElseThrow();
        boolean hasPublished = releaseRepository.existsByTranslationIdAndStatus(translationId, "published");
        if (hasPublished && "draft".equals(translation.getStatus())) {
            translation.setStatus("in_progress");
            translationRepository.save(translation);
        } else if (!hasPublished && !"draft".equals(translation.getStatus())) {
            translation.setStatus("draft");
            translationRepository.save(translation);
        }
    }

    private String normalizeReleaseStatus(String raw) {
        if ("published".equals(raw) || "completed".equals(raw))
            return "published";
        return "draft";
    }
}