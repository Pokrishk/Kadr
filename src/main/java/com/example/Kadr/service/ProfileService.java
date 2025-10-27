package com.example.Kadr.service;

import com.example.Kadr.model.Organizer;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.OrganizerRepository;
import com.example.Kadr.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppUserDetailsService uds;
    private final Validator validator;
    private final OrganizerRepository organizers;

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

    @Transactional(readOnly = true)
    public boolean hasOrganizer(User user) {
        return organizers.existsByUser(user);
    }

    @Transactional
    public Organizer createOrganizerForUser(User user, String organizationName, String contactEmail) {
        if (organizers.existsByUser(user)) {
            throw new IllegalArgumentException("За вашим профилем уже закреплён организатор.");
        }

        Organizer org = Organizer.builder()
                .user(user)
                .organizationName(organizationName == null ? null : organizationName.trim())
                .contactEmail(contactEmail == null ? null : contactEmail.trim())
                .build();

        Set<ConstraintViolation<Organizer>> violations = validator.validate(org);
        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new IllegalArgumentException(message);
        }

        return organizers.save(org);
    }
}
