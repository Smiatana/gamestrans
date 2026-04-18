package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.Issue;

public interface IssueRepository extends JpaRepository<Issue, UUID> {
    List<Issue> findByTranslationIdOrderByCreatedAtDesc(UUID translationId);

    void deleteByTranslationId(UUID translationId);
}
