package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smiatana.gamestrans.entity.TranslationMember;

public interface TranslationMemberRepository extends JpaRepository<TranslationMember, UUID> {
    @Query("SELECT tm FROM TranslationMember tm JOIN FETCH tm.user WHERE tm.translation.id = :translationId")
    List<TranslationMember> findByTranslationId(@Param("translationId") UUID translationId);

    List<TranslationMember> findUserById(UUID userId);

    @Query("SELECT tm FROM TranslationMember tm JOIN FETCH tm.translation t JOIN FETCH t.game WHERE tm.user.email = :email")
    List<TranslationMember> findByUserEmail(String email);

    boolean existsByTranslationIdAndUserEmailAndRole(UUID translationId, String email, String role);

    void deleteByTranslationId(UUID translationId);

    void deleteByUserId(UUID userId);
}
