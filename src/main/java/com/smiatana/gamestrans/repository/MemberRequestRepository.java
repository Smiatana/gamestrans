package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.MemberRequest;

public interface MemberRequestRepository extends JpaRepository<MemberRequest, UUID> {
    boolean existsByTranslationIdAndToUserIdAndStatus(UUID translationId, UUID toUserId, String status);

    List<MemberRequest> findByToUserEmailAndStatus(String email, String status);
}
