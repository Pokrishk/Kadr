package com.example.Kadr.service;

import com.example.Kadr.model.*;
import com.example.Kadr.repository.*;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminServiceTest {

    @Mock
    private OrganizerRepository organizerRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LogEntryRepository logEntryRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private Validator validator;

    @InjectMocks
    private AdminService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void approveOrganizerRequestAssignsRoleAndLogs() {
        User user = User.builder().id(4L).username("alice").build();
        Organizer organizer = Organizer.builder().id(1L).user(user).build();
        Role organizerRole = Role.builder().id(2L).title("Organizer").build();
        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(roleRepository.findByTitleIgnoreCase("Organizer")).thenReturn(Optional.of(organizerRole));

        service.approveOrganizerRequest(1L);

        verify(userRepository).save(user);
        verify(auditLogService).log(eq(AuditAction.UPDATE), eq("users"), contains("Назначена роль организатора"));
        assertThat(user.getRole()).isEqualTo(organizerRole);
    }

    @Test
    void findUsersReturnsFilteredPage() {
        User user = User.builder().username("bob").build();
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("bob", "bob", PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<User> result = service.findUsers("bob", PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(user);
    }
}