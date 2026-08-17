package com.projekfajar.settings.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projekfajar.settings.model.AppSetting;

public interface SettingRepository extends JpaRepository<AppSetting, Long> {

    Optional<AppSetting> findByKey(String key);
}