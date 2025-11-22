package com.example.Kadr.service;

import com.example.Kadr.config.ValidationGroups;
import com.example.Kadr.model.Role;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.RoleRepository;
import com.example.Kadr.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private RoleRepository roles;
    @Mock
    private Validator validator;
    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private AuthService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerEntitySavesUserWithRole() {
        User form = User.builder().username("new").email("e@example.com").build();
        Role role = Role.builder().title("User").build();
        when(roles.findByTitleIgnoreCase("User")).thenReturn(Optional.of(role));
        when(validator.validate(any(User.class), eq(ValidationGroups.OnPersist.class))).thenReturn(Collections.emptySet());
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.registerEntity(form, "hash");

        assertThat(saved.getRole()).isEqualTo(role);
    }
}