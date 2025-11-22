package com.example.Kadr.service;

import com.example.Kadr.model.AuditAction;
import com.example.Kadr.model.Organizer;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.OrganizerRepository;
import com.example.Kadr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AppUserDetailsService uds;
    @Mock
    private Validator validator;
    @Mock
    private OrganizerRepository organizers;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProfileService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateEmailValidatesPasswordAndLogs() {
        User user = User.builder().id(1L).username("alice").passwordHash("hash").build();
        when(passwordEncoder.matches("pwd", "hash")).thenReturn(true);

        service.updateEmail(user, "alice@example.com", "pwd");

        verify(users).save(user);
        verify(auditLogService).log(eq(AuditAction.UPDATE), eq("users"), contains("Изменён email"));
    }

    @Test
    void changePasswordFailsOnMismatch() {
        User user = User.builder().passwordHash("hash").build();
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(user, "old", "newpassword", "other"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Пароли не совпадают");
    }
}