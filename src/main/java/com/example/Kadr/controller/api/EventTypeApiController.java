package com.example.Kadr.controller.api;

import com.example.Kadr.model.EventType;
import com.example.Kadr.repository.EventTypeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-types")
@Tag(name = "Типы событий", description = "Справочник типов событий")
public class EventTypeApiController extends AbstractCrudApiController<EventType> {

    private final EventTypeRepository eventTypeRepository;

    public EventTypeApiController(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Override
    protected JpaRepository<EventType, Long> getRepository() {
        return eventTypeRepository;
    }

    @Override
    protected String getResourceName() {
        return "Тип события";
    }
}