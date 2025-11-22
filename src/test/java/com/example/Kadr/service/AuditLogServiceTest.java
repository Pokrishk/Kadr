package com.example.Kadr.service;

import com.example.Kadr.model.*;
import com.example.Kadr.repository.ActionRepository;
import com.example.Kadr.repository.LogEntryRepository;
import com.example.Kadr.repository.UserLogRepository;
import com.example.Kadr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    @Mock
    private LogEntryRepository logEntryRepository;
    @Mock
    private ActionRepository actionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLogRepository userLogRepository;

    @InjectMocks
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void logSkipsWhenActionMissing() {
        service.log(null, "events", "test");
        verifyNoInteractions(logEntryRepository);
    }

    @Test
    void logCreatesActionAndUserLink() {
        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("alice", "pwd", "ROLE_USER");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsernameIgnoreCase("alice"))
                .thenReturn(Optional.of(new User()));

        when(actionRepository.findByTitleIgnoreCase(any()))
                .thenReturn(Optional.empty());
        when(actionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.log(AuditAction.CREATE, "Events", "created");

        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        verify(actionRepository).save(actionCaptor.capture());
        assertThat(actionCaptor.getValue().getTitle()).contains("CREATE:events");

        verify(userLogRepository).save(any());
    }

}