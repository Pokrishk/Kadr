package com.example.Kadr.controller;

import com.example.Kadr.model.Event;
import com.example.Kadr.repository.TicketRepository;
import com.example.Kadr.service.EventService;
import com.example.Kadr.service.EventTypeService;
import com.example.Kadr.service.OrganizerService;
import com.example.Kadr.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class MainController {
    private final EventTypeService eventTypeService;
    private final EventService eventService;
    private final ReviewService reviewService;
    private final OrganizerService organizerService;
    private final TicketRepository ticketRepository;

    @GetMapping
    public String showMain(Model model) {
        model.addAttribute("types",   eventTypeService.getRandomTypes(200, 8));
        model.addAttribute("events",  eventService.getUpcomingRandom(OffsetDateTime.now(), 200, 9));
        model.addAttribute("reviews", reviewService.getLatestFiveStar(6));
        return "main";
    }
    @GetMapping("/events/{id}")
    public String details(@PathVariable Long id, Model model) {
        var event = eventService.findByIdWithRelations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("event", event);
        model.addAttribute("reviews", reviewService.getForEvent(id, 20));
        model.addAttribute("tickets", ticketRepository.findByEvent_Id(id)); // NEW
        return "event-details";
    }
    @GetMapping("/about")
    public String about() {
        return "about";
    }
    @GetMapping("/organizers")
    public String listOrganizers(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 12, sort = "organizationName", direction = Sort.Direction.ASC) Pageable pageable,
            Model model
    ) {
        var page = organizerService.list(q, pageable);
        model.addAttribute("page", page);
        model.addAttribute("q", q);
        return "organizers";
    }

    @GetMapping("/events")
    public String browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime timeFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime timeTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "asc") String sort,
            Model model
    ) {
        Page<Event> events = eventService.search(
                q, typeId, organizerId, dateFrom, dateTo, timeFrom, timeTo, page, size, sort
        );

        model.addAttribute("events", events);
        model.addAttribute("types", eventTypeService.findAll());
        model.addAttribute("organizers", organizerService.findAll());

        model.addAttribute("q", q);
        model.addAttribute("typeId", typeId);
        model.addAttribute("organizerId", organizerId);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("timeFrom", timeFrom);
        model.addAttribute("timeTo", timeTo);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size);

        return "events";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
