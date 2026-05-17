package com.smiatana.gamestrans.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.IssueRequest;
import com.smiatana.gamestrans.entity.Issue;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.IssueRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueService {
    private final IssueRepository issueRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final com.smiatana.gamestrans.service.AuditLogService auditLogService;

    public Issue create(IssueRequest req, User author, UUID translationId) {
        Issue issue = new Issue();
        issue.setTranslation(translationRepository.findById(translationId).orElseThrow());
        issue.setAuthor(author);
        issue.setTitle(req.getTitle());
        issue.setDescription(req.getDescription());
        issue.setReleaseTitle(req.getReleaseTitle());
        Issue saved = issueRepository.save(issue);
        auditLogService.log(author, "ISSUE_CREATE", "issue", saved.getId(),
                "Праблема '" + saved.getTitle() + "' створана карыстальнікам " + author.getUsername());
        return saved;
    }

    public Issue update(UUID id, IssueRequest req, User currentUser) {
        Issue issue = issueRepository.findById(id).orElseThrow();
        if (!issue.getAuthor().getId().equals(currentUser.getId()))
            throw new IllegalArgumentException("Няма правоў");
        issue.setTitle(req.getTitle());
        issue.setDescription(req.getDescription());
        issue.setReleaseTitle(req.getReleaseTitle());
        Issue saved = issueRepository.save(issue);
        auditLogService.log(currentUser, "ISSUE_UPDATE", "issue", saved.getId(),
            "Праблема '" + saved.getTitle() + "' абноўлена карыстальнікам " + currentUser.getUsername());
        return saved;
    }

    public Issue setStatus(UUID id, String status, User currentUser, UUID translationId) {
        if (!translationMemberRepository.existsByTranslationIdAndUserEmail(translationId, currentUser.getEmail()))
            throw new IllegalArgumentException("Няма правоў");
        Issue issue = issueRepository.findById(id).orElseThrow();
        issue.setStatus(status);
        Issue saved = issueRepository.save(issue);
        auditLogService.log(currentUser, "ISSUE_STATUS", "issue", saved.getId(),
            "Статус праблемы '" + saved.getTitle() + "' зменены на '" + status + "' карыстальнікам " + currentUser.getUsername());
        return saved;
    }

    public void delete(UUID id, User currentUser, UUID translationId) {
        Issue issue = issueRepository.findById(id).orElseThrow();
        boolean isAuthor = issue.getAuthor().getId().equals(currentUser.getId());
        boolean isMember = translationMemberRepository
                .existsByTranslationIdAndUserEmail(translationId, currentUser.getEmail());
        if (!isAuthor && !isMember)
            throw new IllegalArgumentException("Няма правоў");
        issueRepository.deleteById(id);
        auditLogService.log(currentUser, "ISSUE_DELETE", "issue", id,
            "Праблема выдалена карыстальнікам " + currentUser.getUsername());
    }
}