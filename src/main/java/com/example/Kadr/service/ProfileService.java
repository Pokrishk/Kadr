package com.example.Kadr.service;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppUserDetailsService uds;

    private void verifyPassword(User user, String raw) {
        if (raw == null || !passwordEncoder.matches(raw, user.getPasswordHash())) {
            throw new IllegalArgumentException("Неверный текущий пароль");
        }
    }

    private void refreshAuthentication(String username) {
        var userDetails = uds.loadUserByUsername(username);
        var auth = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Transactional
    public void updateUsername(User user, String newUsername, String currentPassword) {
        verifyPassword(user, currentPassword);
        if (newUsername == null || newUsername.isBlank() || newUsername.length() > 100) {
            throw new IllegalArgumentException("Некорректный логин");
        }
        users.findByUsername(newUsername).ifPresent(u -> {
            if (!u.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Такой логин уже занят");
            }
        });
        user.setUsername(newUsername);
        users.save(user);
        refreshAuthentication(user.getUsername());
    }

    @Transactional
    public void updateEmail(User user, String newEmail, String currentPassword) {
        verifyPassword(user, currentPassword);
        if (newEmail == null || newEmail.isBlank() || newEmail.length() > 320 || !newEmail.contains("@")) {
            throw new IllegalArgumentException("Некорректный email");
        }
        users.findByEmail(newEmail).ifPresent(u -> {
            if (!u.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Такой email уже зарегистрирован");
            }
        });
        user.setEmail(newEmail);
        users.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword, String confirmNewPassword) {
        verifyPassword(user, currentPassword);
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Новый пароль минимум 8 символов");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        refreshAuthentication(user.getUsername());
    }
}
