package com.example.Kadr.service;

import com.example.Kadr.model.User;
import com.example.Kadr.model.UserSettings;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.repository.UserSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    public record SettingOption(String value, String label) {}

    private static final Set<String> THEMES = Set.of("system", "dark", "light");
    private static final Map<String, String> FONT_FAMILIES = Map.of(
            "system-ui", "system-ui",
            "sans-serif", "sans-serif",
            "serif", "serif",
            "monospace", "monospace"
    );
    private static final Set<Integer> PAGE_SIZES = Set.of(10, 20, 50, 100);
    private static final Pattern FILTER_KEY = Pattern.compile("^[a-z0-9_-]{1,60}$");

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UserSettings ensureSettingsForUser(User user) {
        return settingsRepository.findByUser(user)
                .orElseGet(() -> settingsRepository.save(UserSettings.builder()
                        .user(user)
                        .theme("system")
                        .fontFamily("system-ui")
                        .fontSize(14)
                        .pageSize(20)
                        .savedFilters(objectMapper.createObjectNode())
                        .build()));
    }

    @Transactional
    public UserSettings ensureSettingsForUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + username));
        return ensureSettingsForUser(user);
    }

    @Transactional
    public UserSettings updatePreferences(User user, String theme, String fontFamily, Integer fontSize, Integer pageSize) {
        UserSettings settings = ensureSettingsForUser(user);
        settings.setTheme(normalizeTheme(theme));
        settings.setFontFamily(normalizeFontFamily(fontFamily));
        settings.setFontSize(validateFontSize(fontSize));
        settings.setPageSize(validatePageSize(pageSize));
        return settingsRepository.save(settings);
    }

    @Transactional
    public UserSettings saveFilters(User user, String key, JsonNode filters) {
        UserSettings settings = ensureSettingsForUser(user);
        String normalizedKey = normalizeFilterKey(key);
        ObjectNode current = settings.getSavedFilters() == null
                ? objectMapper.createObjectNode()
                : settings.getSavedFilters().deepCopy();

        if (filters == null || filters.isNull() || (filters.isObject() && filters.isEmpty())) {
            current.remove(normalizedKey);
        } else {
            if (!filters.isObject()) {
                current.remove(normalizedKey);
            } else {
                current.set(normalizedKey, ((ObjectNode) filters).deepCopy());
            }
        }
        settings.setSavedFilters(current);
        return settingsRepository.save(settings);
    }

    @Transactional
    public void ensureSettingsForAllUsers() {
        userRepository.findAll().forEach(this::ensureSettingsForUser);
    }

    @Transactional
    public UserSettings clearFilters(User user, String key) {
        return saveFilters(user, key, JsonNodeFactory.instance.objectNode());
    }

    @Transactional
    public UserSettings clearFiltersForUsername(String username, String key) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Имя пользователя обязательно для сброса фильтров");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + username));
        return clearFilters(user, key);
    }

    @Transactional
    public UserSettings overwriteFilters(User user, String key, ObjectNode filters) {
        return saveFilters(user, key, filters);
    }

    public Optional<UserSettings> findByUsername(String username) {
        return settingsRepository.findByUser_UsernameIgnoreCase(username);
    }

    public JsonNode getSavedFilters(UserSettings settings, String key) {
        if (settings == null || settings.getSavedFilters() == null) {
            return null;
        }
        JsonNode node = settings.getSavedFilters().get(key);
        return node != null && !node.isNull() ? node : null;
    }

    public List<SettingOption> getThemeOptions() {
        return List.of(
                new SettingOption("system", "Системная"),
                new SettingOption("dark", "Тёмная"),
                new SettingOption("light", "Светлая")
        );
    }

    public List<SettingOption> getFontOptions() {
        return List.of(
                new SettingOption("system-ui", "Системный"),
                new SettingOption("sans-serif", "Без засечек"),
                new SettingOption("serif", "С засечками"),
                new SettingOption("monospace", "Моноширинный")
        );
    }

    public Map<String, String> getFontFamilyStacks() {
        return Map.of(
                "system-ui", "system-ui, -apple-system, 'Segoe UI', Roboto, Arial, sans-serif",
                "sans-serif", "'Inter', 'Roboto', 'Helvetica Neue', Arial, sans-serif",
                "serif", "Georgia, 'Times New Roman', serif",
                "monospace", "'JetBrains Mono', 'Fira Code', 'SFMono-Regular', monospace"
        );
    }

    private String normalizeTheme(String theme) {
        String value = theme == null ? "system" : theme.trim().toLowerCase(Locale.ROOT);
        if (!THEMES.contains(value)) {
            throw new IllegalArgumentException("Некорректная тема оформления");
        }
        return value;
    }

    private String normalizeFontFamily(String fontFamily) {
        String value = fontFamily == null ? "system-ui" : fontFamily.trim().toLowerCase(Locale.ROOT);
        if (!FONT_FAMILIES.containsKey(value)) {
            throw new IllegalArgumentException("Некорректное семейство шрифтов");
        }
        return value;
    }

    private int validateFontSize(Integer size) {
        int value = size == null ? 14 : size;
        if (value < 10 || value > 24) {
            throw new IllegalArgumentException("Размер шрифта должен быть от 10 до 24");
        }
        return value;
    }

    private int validatePageSize(Integer size) {
        int value = size == null ? 20 : size;
        if (!PAGE_SIZES.contains(value)) {
            throw new IllegalArgumentException("Размер страницы должен быть 10, 20, 50 или 100 записей");
        }
        return value;
    }

    private String normalizeFilterKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Ключ фильтра обязателен");
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (!FILTER_KEY.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Некорректный идентификатор фильтра");
        }
        return normalized;
    }
}