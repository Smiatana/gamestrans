package com.smiatana.gamestrans.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.CreateReleaseRequest;
import com.smiatana.gamestrans.entity.Release;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.ReleaseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final FileStorageService fileStorageService;

    private final ReleaseRepository releaseRepository;

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
        Release release = new Release();
        release.setTitle(req.getTitle());
        release.setFileUrl(fileUrl);
        release.setDescription(req.getDescription());
        release.setTranslation(translation);
        release.setCreatedBy(currentUser);

        return releaseRepository.save(release);
    }

}
