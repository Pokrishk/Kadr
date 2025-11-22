package com.example.Kadr.service;

import com.example.Kadr.model.User;
import com.example.Kadr.model.UserSettings;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.repository.UserSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository settingsRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserSettingsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(objectMapper.createObjectNode()).thenReturn(JsonNodeFactory.instance.objectNode());
    }

    @Test
    void ensureSettingsCreatesDefaults() {
        User user = new User();
        when(settingsRepository.findByUser(user)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(UserSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSettings settings = service.ensureSettingsForUser(user);

        assertThat(settings.getTheme()).isEqualTo("system");
        verify(settingsRepository).save(any(UserSettings.class));
    }

    @Test
    void saveFiltersClearsWhenNull() {
        User user = new User();
        UserSettings settings = UserSettings.builder()
                .user(user)
                .savedFilters(JsonNodeFactory.instance.objectNode())
                .build();
        when(settingsRepository.findByUser(user)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSettings result = service.saveFilters(user, "list", null);

        assertThat(result.getSavedFilters().has("list")).isFalse();
    }
}