package com.smiatana.gamestrans.service;

import java.io.IOException;
import java.util.Map;
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
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

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

        String status = "on_review";

        Release release = new Release();
        release.setTitle(req.getTitle());
        release.setFileUrl(fileUrl);
        release.setDescription(req.getDescription());
        release.setTranslation(translation);
        release.setCreatedBy(currentUser);
        release.setStatus(status);

        Release saved = releaseRepository.save(release);
        auditLogService.log(currentUser, "RELEASE_CREATE", "release", saved.getId(),
                "Release '" + saved.getTitle() + "' submitted for review");
        return saved;
    }

    @Transactional
    public Release update(UUID releaseId, CreateReleaseRequest req, User currentUser) throws IOException {
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

        Release release = releaseRepository.findById(releaseId).orElseThrow();
        release.setTitle(req.getTitle());
        release.setFileUrl(fileUrl);
        release.setDescription(req.getDescription());
        release.setStatus("on_review");

        Release saved = releaseRepository.save(release);
        syncTranslationVisibility(release.getTranslation().getId());
        auditLogService.log(currentUser, "RELEASE_UPDATE", "release", saved.getId(),
                "Release '" + saved.getTitle() + "' updated and resubmitted for review");
        return saved;
    }

    @Transactional
    public void delete(UUID releaseId, User currentUser) {
        Release release = releaseRepository.findById(releaseId).orElseThrow();
        
        if (release.getFileUrl() != null && !release.getFileUrl().isBlank()) {
            try {
                fileStorageService.delete(release.getFileUrl());
            } catch (Exception e) {
                auditLogService.log(currentUser, "FILE_DELETE_ERROR", "release", releaseId,
                        "Failed to delete file: " + release.getFileUrl());
            }
        }
        
        releaseRepository.delete(release);
        syncTranslationVisibility(release.getTranslation().getId());
        
        auditLogService.log(currentUser, "RELEASE_DELETE_PERMANENT", "release", releaseId,
                "Release '" + release.getTitle() + "' permanently deleted by " + currentUser.getUsername());
    }


    @Transactional
    public Release approve(UUID releaseId, User moderator) {
        Release release = releaseRepository.findById(releaseId).orElseThrow();
        release.setStatus("published");
        Release saved = releaseRepository.save(release);
        syncTranslationVisibility(release.getTranslation().getId());

        notificationService.send(release.getCreatedBy(), "release_approved", Map.of(
                "releaseTitle", release.getTitle(),
                "translationTitle", release.getTranslation().getTitle(),
                "gameTitle", release.getTranslation().getGame().getTitle()));

        auditLogService.log(moderator, "RELEASE_APPROVE", "release", saved.getId(),
                "Release '" + saved.getTitle() + "' approved by " + moderator.getUsername());
        return saved;
    }

    @Transactional
    public Release reject(UUID releaseId, User moderator, String note) {
        Release release = releaseRepository.findById(releaseId).orElseThrow();
        release.setStatus("hidden");
        Release saved = releaseRepository.save(release);
        syncTranslationVisibility(release.getTranslation().getId());

        notificationService.send(release.getCreatedBy(), "release_rejected", Map.of(
                "releaseTitle", release.getTitle(),
                "translationTitle", release.getTranslation().getTitle(),
                "gameTitle", release.getTranslation().getGame().getTitle(),
                "note", note != null ? note : ""));

        auditLogService.log(moderator, "RELEASE_REJECT", "release", saved.getId(),
                "Release '" + saved.getTitle() + "' rejected by " + moderator.getUsername());
        return saved;
    }

    @Transactional
    public void softDelete(UUID releaseId, User moderator) {
        Release release = releaseRepository.findById(releaseId).orElseThrow();
        release.setStatus("deleted");
        release.setDeletedAt(java.time.LocalDateTime.now());
        releaseRepository.save(release);
        syncTranslationVisibility(release.getTranslation().getId());
        auditLogService.log(moderator, "RELEASE_DELETE", "release", releaseId,
                "Release '" + release.getTitle() + "' deleted by " + moderator.getUsername());
    }

    private void syncTranslationVisibility(UUID translationId) {
        Translation translation = translationRepository.findById(translationId).orElseThrow();
        boolean hasPublished = releaseRepository.existsByTranslationIdAndStatus(translationId, "published");
        if (hasPublished && "draft".equals(translation.getStatus())) {
            translation.setStatus("in_progress");
            translationRepository.save(translation);
        } else if (!hasPublished && !("draft".equals(translation.getStatus()))) {
            translation.setStatus("draft");
            translationRepository.save(translation);
        }
    }
}