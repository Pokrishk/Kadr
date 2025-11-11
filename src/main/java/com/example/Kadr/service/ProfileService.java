package com.example.Kadr.service;

import com.example.Kadr.model.AuditAction;
import com.example.Kadr.model.Organizer;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.OrganizerRepository;
import com.example.Kadr.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppUserDetailsService uds;
    private final Validator validator;
    private final OrganizerRepository organizers;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_THEMES = Set.of("light", "dark", "system");
    private static final Set<String> ALLOWED_DATE_FORMATS =
            Set.of("dd.MM.yyyy", "MM/dd/yyyy", "yyyy-MM-dd");
    private static final Set<String> ALLOWED_NUMBER_FORMATS = Set.of("comma", "dot");
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50);

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
        auditLogService.log(
                AuditAction.UPDATE,
                "users",
                String.format("Изменён логин пользователя ID=%d на %s", user.getId(), user.getUsername())
        );
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
        auditLogService.log(
                AuditAction.UPDATE,
                "users",
                String.format("Изменён email пользователя %s (ID=%d)", user.getUsername(), user.getId())
        );
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
        auditLogService.log(
                AuditAction.UPDATE,
                "users",
                String.format("Изменён пароль пользователя %s (ID=%d)", user.getUsername(), user.getId())
        );
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

        Organizer saved = organizers.save(org);
        auditLogService.log(
                AuditAction.CREATE,
                "organizers",
                String.format("Создан организатор ID=%d для пользователя %s", saved.getId(), user.getUsername())
        );
        return saved;
    }

    @Transactional
    public void updatePreferences(User user,
                                  String theme,
                                  String dateFormat,
                                  String numberFormat,
                                  Integer pageSize,
                                  String savedFilters) {
        if (!ALLOWED_THEMES.contains(theme)) {
            throw new IllegalArgumentException("Некорректная тема интерфейса");
        }
        if (!ALLOWED_DATE_FORMATS.contains(dateFormat)) {
            throw new IllegalArgumentException("Некорректный формат даты");
        }
        if (!ALLOWED_NUMBER_FORMATS.contains(numberFormat)) {
            throw new IllegalArgumentException("Некорректный формат чисел");
        }
        if (pageSize == null || !ALLOWED_PAGE_SIZES.contains(pageSize)) {
            throw new IllegalArgumentException("Некорректный размер страниц");
        }

        String normalizedFilters = savedFilters == null ? "[]" : savedFilters.trim();
        if (normalizedFilters.isEmpty()) {
            normalizedFilters = "[]";
        }
        if (normalizedFilters.length() > 10_000) {
            throw new IllegalArgumentException("Слишком большой объём сохранённых фильтров");
        }

        try {
            JsonNode filtersNode = objectMapper.readTree(normalizedFilters);
            if (!filtersNode.isArray()) {
                throw new IllegalArgumentException("Сохранённые фильтры должны быть массивом");
            }
            normalizedFilters = objectMapper.writeValueAsString(filtersNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Некорректный формат сохранённых фильтров");
        }

        Map<String, Object> previous = Map.of(
                "theme", user.getTheme(),
                "dateFormat", user.getDateFormat(),
                "numberFormat", user.getNumberFormat(),
                "pageSize", user.getPageSize(),
                "savedFilters", user.getSavedFilters()
        );

        user.setTheme(theme);
        user.setDateFormat(dateFormat);
        user.setNumberFormat(numberFormat);
        user.setPageSize(pageSize);
        user.setSavedFilters(normalizedFilters);
        users.save(user);

        auditLogService.log(
                AuditAction.UPDATE,
                "users",
                String.format(
                        "Обновлены пользовательские настройки ID=%d (было: %s)",
                        user.getId(), previous
                )
        );
    }
}
