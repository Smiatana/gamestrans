package com.smiatana.gamestrans.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.entity.MemberRequest;
import com.smiatana.gamestrans.entity.Translation;
import com.smiatana.gamestrans.entity.TranslationMember;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.MemberRequestRepository;
import com.smiatana.gamestrans.repository.NotificationRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;
import com.smiatana.gamestrans.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberRequestService {
    private final TranslationRepository translationRepository;
    private final UserRepository userRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final MemberRequestRepository memberRequestRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final com.smiatana.gamestrans.service.AuditLogService auditLogService;

    @Transactional
    public void sendInvite(UUID translationId, String fromEmail, String toUsername) {
        List<User> users = userRepository.findAllByUsername(toUsername);
        Translation translation = translationRepository.findById(translationId).orElseThrow();
        User fromUser = userRepository.findByEmail(fromEmail).orElseThrow();
        User toUser = userRepository.findByUsername(toUsername).orElseThrow();

        if (translationMemberRepository.existsByTranslationIdAndUserEmail(translationId, toUser.getEmail())) {
            throw new IllegalArgumentException("Карыстальнік ужо з'яўляецца ўдзельнікам");
        }

        if (memberRequestRepository.existsByTranslationIdAndToUserIdAndStatus(translationId, toUser.getId(),
                "pending")) {
            throw new IllegalArgumentException("Запрашэнне ўжо адпраўлена");
        }

        MemberRequest request = new MemberRequest();
        request.setTranslation(translation);
        request.setFromUser(fromUser);
        request.setToUser(toUser);
        memberRequestRepository.save(request);

        notificationService.send(toUser, "member_request", Map.of(
                "requestId", request.getId().toString(),
                "translationTitle", translation.getTitle(),
                "gameTitle", translation.getGame().getTitle(),
                "fromUsername", fromUser.getUsername()));
        auditLogService.log(fromUser, "MEMBER_REQUEST_SEND", "member_request", request.getId(),
            "Запрашэнне на пераклад '" + translation.getTitle() + "' адпраўлена карыстальніку " + toUser.getUsername());

    }

    @Transactional
    public void accept(UUID requestId, String userEmail) {
        MemberRequest request = memberRequestRepository.findById(requestId).orElseThrow();

        if (!request.getToUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Няма дазволу");
        }

        request.setStatus("accepted");
        memberRequestRepository.save(request);

        notificationRepository.deleteByTypeAndPayloadContaining(
                "member_request",
                request.getId().toString());

        TranslationMember member = new TranslationMember();
        member.setTranslation(request.getTranslation());
        member.setUser(request.getToUser());
        member.setRole("member");
        translationMemberRepository.save(member);

        notificationService.send(request.getFromUser(), "request_accepted", Map.of(
                "translationTitle", request.getTranslation().getTitle(),
                "gameTitle", request.getTranslation().getGame().getTitle(),
                "username", request.getToUser().getUsername()));
        auditLogService.log(request.getToUser(), "MEMBER_REQUEST_ACCEPT", "member_request", request.getId(),
            "Запрашэнне прынята карыстальнікам " + request.getToUser().getUsername());

    }

    @Transactional
    public void reject(UUID reqestId, String userEmail) {
        MemberRequest request = memberRequestRepository.findById(reqestId).orElseThrow();

        if (!request.getToUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Няма дазволу");
        }

        request.setStatus("rejected");
        memberRequestRepository.save(request);

        notificationRepository.deleteByTypeAndPayloadContaining(
                "member_request",
                request.getId().toString());

        notificationService.send(request.getFromUser(), "request_rejected", Map.of(
                "translationTitle", request.getTranslation().getTitle(),
                "gameTitle", request.getTranslation().getGame().getTitle(),
                "username", request.getToUser().getUsername()));
        auditLogService.log(request.getToUser(), "MEMBER_REQUEST_REJECT", "member_request", request.getId(),
            "Запрашэнне адхілена карыстальнікам " + request.getToUser().getUsername());
    }

    @Transactional
    public void kick(UUID translationId, String ownerEmail, String targetUsername) {
        if (!translationMemberRepository.existsByTranslationIdAndUserEmailAndRole(
                translationId, ownerEmail, "owner")) {
            throw new IllegalArgumentException("Няма дазволу");
        }

        User target = userRepository.findByUsername(targetUsername).orElseThrow();
        translationMemberRepository.deleteByTranslationIdAndUserEmail(translationId, target.getEmail());
        Translation translation = translationRepository.findById(translationId).orElseThrow();

        notificationService.send(target, "kicked", Map.of(
                "translationTitle", translation.getTitle(),
                "gameTitle", translation.getGame().getTitle()));
        User owner = userRepository.findByEmail(ownerEmail).orElse(null);
        if (owner != null) {
            auditLogService.log(owner, "MEMBER_KICK", "translation", translationId,
                "Карыстальнік " + target.getUsername() + " выдалены з перакладу '" + translation.getTitle() + "' карыстальнікам " + owner.getUsername());
        }
    }

    public List<MemberRequest> getPendingForUser(String email) {
        return memberRequestRepository.findByToUserEmailAndStatus(email, "pending");
    }

}
