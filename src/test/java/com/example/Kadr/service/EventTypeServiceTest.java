package com.example.Kadr.service;

import com.example.Kadr.model.EventType;
import com.example.Kadr.repository.EventTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class EventTypeServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EventTypeService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRandomTypesReturnsLimitedList() {
        Page<EventType> page = new PageImpl<>(List.of(new EventType()));
        when(eventTypeRepository.findAll(PageRequest.of(0, 3, Sort.by("id").descending()))).thenReturn(page);

        List<EventType> result = service.getRandomTypes(3, 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void importFromSqlRejectsNullStream() {
        assertThatThrownBy(() -> service.importFromSql(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Файл не может быть пустым");
    }
}