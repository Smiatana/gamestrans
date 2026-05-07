package com.smiatana.gamestrans.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.Complaint;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.entity.Warning;
import com.smiatana.gamestrans.repository.ComplaintRepository;
import com.smiatana.gamestrans.repository.UserRepository;
import com.smiatana.gamestrans.repository.WarningRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModerationService {
    private final UserRepository userRepository;
    private final WarningRepository warningRepository;
    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public void banUser(UUID targetId, User moderator, Integer durationDays, String reason, String note) {
        User target = userRepository.findById(targetId).orElseThrow();
        LocalDateTime until = durationDays != null
                ? LocalDateTime.now().plusDays(durationDays)
                : null; // null = permanent

        target.setStatus("banned");
        target.setBannedUntil(until);
        target.setBanReason(reason);
        target.setBanNote(note);
        userRepository.save(target);

        emailService.sendBanNotification(target);
        auditLogService.log(moderator, "USER_BAN", "user", targetId,
                "User '" + target.getUsername() + "' banned by " + moderator.getUsername()
                        + (durationDays != null ? " for " + durationDays + " days" : " permanently"));
    }

    @Transactional
    public void unbanUser(UUID targetId, User moderator) {
        User target = userRepository.findById(targetId).orElseThrow();
        target.setStatus("active");
        target.setBannedUntil(null);
        target.setBanReason(null);
        target.setBanNote(null);
        userRepository.save(target);
        auditLogService.log(moderator, "USER_UNBAN", "user", targetId,
                "User '" + target.getUsername() + "' unbanned by " + moderator.getUsername());
    }

    @Transactional
    public void warnUser(UUID targetId, User moderator, String reason) {
        User target = userRepository.findById(targetId).orElseThrow();

        Warning warning = new Warning();
        warning.setUser(target);
        warning.setIssuedBy(moderator);
        warning.setReason(reason);
        warningRepository.save(warning);

        notificationService.send(target, "warning", Map.of(
                "reason", reason,
                "moderatorUsername", moderator.getUsername()));

        emailService.sendWarningNotification(target, reason);
        auditLogService.log(moderator, "USER_WARN", "user", targetId,
                "User '" + target.getUsername() + "' warned by " + moderator.getUsername());
    }

    @Transactional
    public Complaint submitReport(User author, String targetType, UUID targetId, String reason) {
        if (complaintRepository.existsByAuthorIdAndTargetTypeAndTargetId(author.getId(), targetType, targetId))
            throw new IllegalArgumentException("Вы ўжо паведамілі пра гэты аб'ект");

        Complaint complaint = new Complaint();
        complaint.setAuthor(author);
        complaint.setTargetType(targetType);
        complaint.setTargetId(targetId);
        complaint.setReason(reason);
        return complaintRepository.save(complaint);
    }

    @Transactional
    public Complaint resolveReport(UUID complaintId, User moderator, String decision, String note) {
        // decision: "approved" | "declined"
        Complaint complaint = complaintRepository.findById(complaintId).orElseThrow();
        complaint.setStatus(decision);
        complaint.setResolvedBy(moderator);
        complaint.setModeratorNote(note);
        complaint.setResolvedAt(LocalDateTime.now());
        complaintRepository.save(complaint);

        notificationService.send(complaint.getAuthor(), "report_resolved", Map.of(
                "targetType", complaint.getTargetType(),
                "decision", decision,
                "note", note != null ? note : ""));

        auditLogService.log(moderator, "REPORT_RESOLVE", "report", complaintId,
                "Report resolved as '" + decision + "' by " + moderator.getUsername());
        return complaint;
    }

    @Transactional
    public void deleteReport(UUID complaintId, User moderator) {
        complaintRepository.deleteById(complaintId);
        auditLogService.log(moderator, "REPORT_DELETE", "report", complaintId,
                "Report deleted by " + moderator.getUsername());
    }
}