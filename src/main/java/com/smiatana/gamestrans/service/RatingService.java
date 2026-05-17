package com.smiatana.gamestrans.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.Rating;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.RatingRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final TranslationRepository translationRepository;
    private final com.smiatana.gamestrans.service.AuditLogService auditLogService;
    private final TranslationMemberRepository translationMemberRepository;

    public Rating upsert(UUID translationId, User user, int stars) {
        if (stars < 1 || stars > 5)
            throw new IllegalArgumentException("Ацэнка павінна быць ад 1 да 5");
        if (translationMemberRepository.existsByTranslationIdAndUserEmail(translationId, user.getEmail()))
            throw new IllegalArgumentException("Удзельнікі перакладу не могуць яго ацэньваць");

        Rating rating = ratingRepository
                .findByTranslationIdAndUserId(translationId, user.getId())
                .orElseGet(() -> {
                    Rating r = new Rating();
                    r.setTranslation(translationRepository.findById(translationId).orElseThrow());
                    r.setUser(user);
                    return r;
                });
        rating.setStars(stars);
        Rating saved = ratingRepository.save(rating);
        auditLogService.log(user, "RATING_UPSERT", "rating", saved.getId(),
            "Ацэнка " + stars + " зор(ак) дадзена карыстальнікам " + user.getUsername() + " для перакладу '" + saved.getTranslation().getTitle() + "'");
        return saved;
    }

    public void delete(UUID translationId, User user) {
        Rating rating = ratingRepository
                .findByTranslationIdAndUserId(translationId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ацэнка не знойдзена"));
        ratingRepository.delete(rating);
        auditLogService.log(user, "RATING_DELETE", "rating", rating.getId(),
            "Ацэнка выдалена карыстальнікам " + user.getUsername() + " для перакладу '" + rating.getTranslation().getTitle() + "'");
    }
}