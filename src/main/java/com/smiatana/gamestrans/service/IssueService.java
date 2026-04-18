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

    public Issue create(IssueRequest req, User author, UUID translationId) {
        Issue issue = new Issue();
        issue.setTranslation(translationRepository.findById(translationId).orElseThrow());
        issue.setAuthor(author);
        issue.setTitle(req.getTitle());
        issue.setDescription(req.getDescription());
        issue.setReleaseTitle(req.getReleaseTitle());
        return issueRepository.save(issue);
    }

    public Issue update(UUID id, IssueRequest req, User currentUser) {
        Issue issue = issueRepository.findById(id).orElseThrow();
        if (!issue.getAuthor().getId().equals(currentUser.getId()))
            throw new IllegalArgumentException("Няма правоў");
        issue.setTitle(req.getTitle());
        issue.setDescription(req.getDescription());
        issue.setReleaseTitle(req.getReleaseTitle());
        return issueRepository.save(issue);
    }

    public Issue setStatus(UUID id, String status, User currentUser, UUID translationId) {
        if (!translationMemberRepository.existsByTranslationIdAndUserEmail(translationId, currentUser.getEmail()))
            throw new IllegalArgumentException("Няма правоў");
        Issue issue = issueRepository.findById(id).orElseThrow();
        issue.setStatus(status);
        return issueRepository.save(issue);
    }

    public void delete(UUID id, User currentUser, UUID translationId) {
        Issue issue = issueRepository.findById(id).orElseThrow();
        boolean isAuthor = issue.getAuthor().getId().equals(currentUser.getId());
        boolean isMember = translationMemberRepository
                .existsByTranslationIdAndUserEmail(translationId, currentUser.getEmail());
        if (!isAuthor && !isMember)
            throw new IllegalArgumentException("Няма правоў");
        issueRepository.deleteById(id);
    }
}