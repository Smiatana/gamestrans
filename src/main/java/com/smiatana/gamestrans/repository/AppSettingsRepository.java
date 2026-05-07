package com.smiatana.gamestrans.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smiatana.gamestrans.entity.AppSettings;

public interface AppSettingsRepository extends JpaRepository<AppSettings, Integer> {
}
