package com.smiatana.gamestrans.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.AuditLog;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    /**
     * Record an audit event.
     *
     * @param actor      the user performing the action (null = system)
     * @param action     e.g. "GAME_CREATE", "RELEASE_DELETE", "USER_BAN"
     * @param targetType e.g. "game", "release", "user", "translation", "comment",
     *                   "issue", "report"
     * @param targetId   UUID of the affected entity
     * @param summary    short human-readable description
     */
    public void log(User actor, String action, String targetType, UUID targetId, String summary) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setSummary(summary);
        auditLogRepository.save(entry);
    }
}