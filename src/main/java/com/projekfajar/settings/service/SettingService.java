package com.projekfajar.settings.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projekfajar.settings.model.AppSetting;
import com.projekfajar.settings.repository.SettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingService {

    private final SettingRepository settingRepository;

    @Transactional(readOnly = true)
    public Map<String, String> getMap() {
        log.info("Fetching all application settings");
        return settingRepository.findAll().stream()
                .collect(Collectors.toMap(
                        AppSetting::getKey,
                        s -> s.getValue() != null ? s.getValue() : ""));
    }

    @Transactional
    public Map<String, String> update(Map<String, String> values) {
        log.info("Updating {} application settings", values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }

            AppSetting setting = settingRepository.findByKey(entry.getKey())
                    .orElseGet(() -> AppSetting.builder().key(entry.getKey()).build());

            setting.setValue(entry.getValue());
            setting.setUpdatedAt(LocalDateTime.now());
            settingRepository.save(setting);
            log.info("Setting updated: {} = {}", entry.getKey(), entry.getValue());
        }
        return getMap();
    }

    @Transactional(readOnly = true)
    public String getValue(String key) {
        return settingRepository.findByKey(key)
                .map(AppSetting::getValue)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        String value = getValue(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer setting for key '{}': {}, returning default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Transactional(readOnly = true)
    public List<String> getList(String key) {
        String value = getValue(key);
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}