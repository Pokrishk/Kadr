package com.example.Kadr.service;

import com.example.Kadr.model.Organizer;
import com.example.Kadr.repository.OrganizerRepository;
import com.example.Kadr.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class OrganizerServiceTest {

    @Mock
    private OrganizerRepository organizerRepository;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private OrganizerService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsesRoleIdAndQuery() {
        when(roleRepository.findByTitleIgnoreCase("Organizer")).thenReturn(Optional.of(com.example.Kadr.model.Role.builder().id(1L).build()));
        Page<Organizer> page = new PageImpl<>(List.of(new Organizer()));
        when(organizerRepository.findByUser_Role_IdAndOrganizationNameContainingIgnoreCase(1L, "rock", PageRequest.of(0, 5)))
                .thenReturn(page);

        Page<Organizer> result = service.list("rock", PageRequest.of(0, 5));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getCurrentOrganizerRequiresAuth() {
        assertThatThrownBy(() -> service.getCurrentOrganizerOrThrow())
                .isInstanceOf(IllegalStateException.class);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "pwd"));
        Organizer organizer = new Organizer();
        when(organizerRepository.findByUser_UsernameIgnoreCase("alice")).thenReturn(Optional.of(organizer));

        assertThat(service.getCurrentOrganizerOrThrow()).isEqualTo(organizer);
    }
}