package com.smiatana.gamestrans.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.UploadedFile;
import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    void deleteByPath(String path);

    Optional<UploadedFile> findByPath(String path);

}
