package com.smiatana.gamestrans.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.PasswordResetToken;
import com.smiatana.gamestrans.entity.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}