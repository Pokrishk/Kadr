package com.example.Kadr.service;

import com.example.Kadr.model.AuditAction;
import com.example.Kadr.model.Event;
import com.example.Kadr.repository.EventRepository;
import com.example.Kadr.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EventService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUpcomingRandomRespectsLimit() {
        Event event = new Event();
        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.findByEventDatetimeAfter(any(), any(PageRequest.class))).thenReturn(page);

        List<Event> result = service.getUpcomingRandom(OffsetDateTime.now(), 5, 1);

        assertThat(result).hasSize(1);
        verify(eventRepository).findByEventDatetimeAfter(any(), any(PageRequest.class));
    }

    @Test
    void saveLogsCreateAndUpdate() {
        Event event = new Event();
        when(eventRepository.save(event)).thenReturn(event);

        service.save(event);
        verify(auditLogService).log(eq(AuditAction.CREATE), eq("events"), contains("Создано"));

        event.setId(10L);
        service.save(event);
        verify(auditLogService).log(eq(AuditAction.UPDATE), eq("events"), contains("Обновлено"));
    }

    @Test
    void deleteByIdLogsWhenExists() {
        when(eventRepository.existsById(3L)).thenReturn(true);

        boolean deleted = service.deleteById(3L);

        assertThat(deleted).isTrue();
        verify(auditLogService).log(eq(AuditAction.DELETE), eq("events"), contains("Удалено"));
    }
}