package com.example.Kadr.controller.api;

import com.example.Kadr.model.Ticket;
import com.example.Kadr.repository.TicketRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Билеты", description = "CRUD-операции с билетами")
public class TicketApiController extends AbstractCrudApiController<Ticket> {

    private final TicketRepository ticketRepository;

    public TicketApiController(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    protected JpaRepository<Ticket, Long> getRepository() {
        return ticketRepository;
    }

    @Override
    protected String getResourceName() {
        return "Билет";
    }
}