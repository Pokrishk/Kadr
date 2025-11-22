package com.example.Kadr.controller.api;

import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import com.example.Kadr.service.UserSettingsService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserSettingsApiControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSettingsService userSettingsService;
    @Mock
    private UserDetails principal;

    @InjectMocks
    private UserSettingsApiController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveFiltersReturnsUnauthorizedWhenNoPrincipal() {
        ResponseEntity<Void> response = controller.saveFilters("test", null, JsonNodeFactory.instance.objectNode());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(userRepository, userSettingsService);
    }

    @Test
    void saveFiltersPersistsDataForAuthenticatedUser() {
        User user = new User();
        when(principal.getUsername()).thenReturn("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        ResponseEntity<Void> response = controller.saveFilters("events", principal, JsonNodeFactory.instance.objectNode());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userSettingsService).saveFilters(user, "events", JsonNodeFactory.instance.objectNode());
    }
}