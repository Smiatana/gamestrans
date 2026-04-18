package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smiatana.gamestrans.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByTranslationIdAndParentIsNullOrderByCreatedAtAsc(UUID translationId);

    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    void deleteByTranslationId(UUID translationId);
}
