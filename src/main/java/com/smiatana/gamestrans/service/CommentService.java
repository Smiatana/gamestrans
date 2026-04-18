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

    public Comment create(CommentRequest req, User author, UUID translationId) {
        Comment comment = new Comment();
        comment.setTranslation(translationRepository.findById(translationId).orElseThrow());
        comment.setAuthor(author);
        comment.setBody(req.getBody());
        if (req.getParentId() != null) {
            comment.setParent(commentRepository.findById(req.getParentId()).orElseThrow());
        }
        return commentRepository.save(comment);
    }

    public Comment update(UUID id, CommentRequest req, User currentUser) {
        Comment comment = commentRepository.findById(id).orElseThrow();
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Няма правоў");
        }
        comment.setBody(req.getBody());
        return commentRepository.save(comment);
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
    }
}
