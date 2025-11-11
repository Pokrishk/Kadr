package com.example.Kadr.repository;

import com.example.Kadr.model.User;
import com.example.Kadr.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUser(User user);
    Optional<UserSettings> findByUser_UsernameIgnoreCase(String username);
    boolean existsByUser(User user);
}