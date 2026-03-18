package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.TranslationMember;

public interface TranslationMemberRepository extends JpaRepository<TranslationMember, UUID> {
    List<TranslationMember> findByTranslationId(UUID translationId);

    List<TranslationMember> findUserById(UUID userId);

    boolean existsByTranslationIdAndUserEmailAndRole(UUID translationId, String email, String role);
}
