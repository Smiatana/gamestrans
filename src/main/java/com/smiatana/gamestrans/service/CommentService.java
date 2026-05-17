package com.smiatana.gamestrans.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.smiatana.gamestrans.dto.CommentRequest;
import com.smiatana.gamestrans.entity.Comment;
import com.smiatana.gamestrans.entity.User;
import com.smiatana.gamestrans.repository.CommentRepository;
import com.smiatana.gamestrans.repository.TranslationMemberRepository;
import com.smiatana.gamestrans.repository.TranslationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMemberRepository translationMemberRepository;
    private final com.smiatana.gamestrans.service.AuditLogService auditLogService;

    public Comment create(CommentRequest req, User author, UUID translationId) {
        Comment comment = new Comment();
        comment.setTranslation(translationRepository.findById(translationId).orElseThrow());
        comment.setAuthor(author);
        comment.setBody(req.getBody());
        if (req.getParentId() != null) {
            comment.setParent(commentRepository.findById(req.getParentId()).orElseThrow());
        }
        Comment saved = commentRepository.save(comment);
        auditLogService.log(author, "COMMENT_CREATE", "comment", saved.getId(),
                "Каментар дададзены карыстальнікам " + author.getUsername());
        return saved;
    }

    public Comment update(UUID id, CommentRequest req, User currentUser) {
        Comment comment = commentRepository.findById(id).orElseThrow();
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Няма правоў");
        }
        comment.setBody(req.getBody());
        Comment saved = commentRepository.save(comment);
        auditLogService.log(currentUser, "COMMENT_UPDATE", "comment", saved.getId(),
                "Каментар абноўлены карыстальнікам " + currentUser.getUsername());
        return saved;
    }

    public void delete(UUID id, User currentUser, UUID translationId) {
        Comment comment = commentRepository.findById(id).orElseThrow();
        boolean isAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isMember = translationMemberRepository.existsByTranslationIdAndUserEmail(translationId,
                currentUser.getEmail());
        if (!isAuthor && !isMember) {
            throw new IllegalArgumentException("Няма праоў");
        }
        commentRepository.deleteById(id);
        auditLogService.log(currentUser, "COMMENT_DELETE", "comment", id,
            "Каментар выдалены карыстальнікам " + currentUser.getUsername());
    }
}
