package com.smiatana.gamestrans.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> findByUsernameContainingIgnoreCase(String username);

    List<User> findAllByUsername(String username);

    List<User> findByRoleOrderByCreatedAtDesc(String role);

    Page<User> findByRoleOrderByCreatedAtDesc(String role, Pageable pageable);

    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}