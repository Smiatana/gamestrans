package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByTargetTypeOrderByCreatedAtDesc(String targetType, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findTop50ByTargetTypeOrderByCreatedAtDesc(String targetType);
}
