package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    Page<Complaint> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Complaint> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByAuthorIdAndTargetTypeAndTargetId(UUID authorId, String targetType, UUID targetId);

    Optional<Complaint> findByAuthorIdAndTargetTypeAndTargetId(UUID authorId, String targetType, UUID targetId);

    List<Complaint> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
}