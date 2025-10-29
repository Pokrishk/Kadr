package com.example.Kadr.controller.api;

import com.example.Kadr.model.Event;
import com.example.Kadr.model.Ticket;
import com.example.Kadr.repository.EventRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@Tag(name = "События", description = "Управление событиями и связанными билетами")
public class EventApiController extends AbstractCrudApiController<Event> {

    private final EventRepository eventRepository;

    public EventApiController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    protected JpaRepository<Event, Long> getRepository() {
        return eventRepository;
    }

    @Override
    protected String getResourceName() {
        return "Событие";
    }

    @Override
    protected void prepareForCreate(Event entity) {
        if (entity.getTickets() != null) {
            for (Ticket ticket : entity.getTickets()) {
                Optional.ofNullable(ticket).ifPresent(t -> t.setEvent(entity));
            }
        }
    }
}