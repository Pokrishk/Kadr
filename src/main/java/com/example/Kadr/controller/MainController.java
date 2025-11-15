package com.example.Kadr.controller;

import com.example.Kadr.model.Event;
import com.example.Kadr.repository.TicketRepository;
import com.example.Kadr.service.EventService;
import com.example.Kadr.service.EventTypeService;
import com.example.Kadr.service.OrganizerService;
import com.example.Kadr.service.ReviewService;
import com.example.Kadr.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Pageable;

import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class MainController {
    private final EventTypeService eventTypeService;
    private final EventService eventService;
    private final ReviewService reviewService;
    private final OrganizerService organizerService;
    private final UserSettingsService userSettingsService;
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
        model.addAttribute("tickets", ticketRepository.findByEvent_Id(id));
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
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request,
            Model model
    ) {
        var userSettings = principal == null ? null
                : userSettingsService.ensureSettingsForUsername(principal.getUsername());

        var params = request.getParameterMap();
        boolean resetFilters = params.containsKey("reset");
        if (userSettings != null && resetFilters && principal != null) {
            userSettingsService.clearFiltersForUsername(principal.getUsername(), "catalog");
        }
        if (userSettings != null && !resetFilters) {
            var saved = userSettingsService.getSavedFilters(userSettings, "catalog");
            if (saved != null) {
                if (!params.containsKey("q") && saved.hasNonNull("q")) {
                    q = saved.get("q").asText();
                }
                if (!params.containsKey("typeId") && saved.hasNonNull("typeId") && saved.get("typeId").canConvertToLong()) {
                    typeId = saved.get("typeId").asLong();
                }
                if (!params.containsKey("organizerId") && saved.hasNonNull("organizerId") && saved.get("organizerId").canConvertToLong()) {
                    organizerId = saved.get("organizerId").asLong();
                }
                if (!params.containsKey("dateFrom") && saved.hasNonNull("dateFrom")) {
                    dateFrom = parseDate(saved.get("dateFrom").asText());
                }
                if (!params.containsKey("dateTo") && saved.hasNonNull("dateTo")) {
                    dateTo = parseDate(saved.get("dateTo").asText());
                }
                if (!params.containsKey("timeFrom") && saved.hasNonNull("timeFrom")) {
                    timeFrom = parseTime(saved.get("timeFrom").asText());
                }
                if (!params.containsKey("timeTo") && saved.hasNonNull("timeTo")) {
                    timeTo = parseTime(saved.get("timeTo").asText());
                }
                if (!params.containsKey("sort") && saved.hasNonNull("sort")) {
                    sort = saved.get("sort").asText();
                }
                if (!params.containsKey("size") && saved.hasNonNull("size") && saved.get("size").isInt()) {
                    size = saved.get("size").asInt();
                }
            }
        }

        if (sort == null || sort.isBlank()) {
            sort = "asc";
        }

        int effectiveSize = (size != null && size > 0) ? size
                : userSettings != null ? userSettings.getPageSize() : 20;
        Page<Event> events = eventService.search(
                q, typeId, organizerId, dateFrom, dateTo, timeFrom, timeTo, page, effectiveSize, sort
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
        model.addAttribute("size", effectiveSize);

        return "events";
    }

    @GetMapping("/events/export")
    public void exportEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime timeFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime timeTo,
            @RequestParam(defaultValue = "asc") String sort,
            HttpServletResponse response
    ) throws IOException {
        List<Event> events = eventService.searchAll(q, typeId, organizerId, dateFrom, dateTo, timeFrom, timeTo, sort);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=events.csv");

        try (var os = response.getOutputStream();
             var writer = new BufferedWriter(new OutputStreamWriter(os, java.nio.charset.StandardCharsets.UTF_8))) {
            os.write(0xEF); os.write(0xBB); os.write(0xBF);
            writer.write("sep=;");
            writer.write("\r\n");
            writer.write("ID;Название;Дата и время;Организатор;Тип;Адрес;Описание;Рейтинг;Билеты всего\r\n");

            Locale locale = Locale.forLanguageTag("ru-RU");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", locale);
            ZoneId zone = ZoneId.systemDefault();

            for (Event event : events) {
                String id = event.getId() != null ? event.getId().toString() : "";
                String date = event.getEventDatetime() != null
                        ? event.getEventDatetime().atZoneSameInstant(zone).format(formatter)
                        : "";
                String organizer = event.getOrganizer() != null ? safe(event.getOrganizer().getOrganizationName()) : "";
                String type = event.getEventType() != null ? safe(event.getEventType().getTitle()) : "";
                String address = formatAddress(event);
                String title = flatten(event.getTitle());
                String description = flatten(event.getDescription());
                String rating = event.getRating() != null ? event.getRating().toPlainString() : "";
                String tickets = event.getTicketsTotal() != null ? event.getTicketsTotal().toString() : "";

                writer.write(String.join(";",
                        escapeCsv(id),
                        escapeCsv(title),
                        escapeCsv(date),
                        escapeCsv(organizer),
                        escapeCsv(type),
                        escapeCsv(address),
                        escapeCsv(description),
                        escapeCsv(rating),
                        escapeCsv(tickets)
                ));
                writer.write("\r\n");
            }

            writer.flush();
        }
    }
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private String formatAddress(Event event) {
        if (event.getAddress() == null) {
            return "";
        }
        var address = event.getAddress();
        var parts = new java.util.ArrayList<String>();
        if (address.getCountry() != null && !address.getCountry().isBlank()) {
            parts.add(address.getCountry().trim());
        }
        if (address.getCity() != null && !address.getCity().isBlank()) {
            parts.add(address.getCity().trim());
        }
        var streetParts = new java.util.ArrayList<String>();
        if (address.getStreet() != null && !address.getStreet().isBlank()) {
            streetParts.add(address.getStreet().trim());
        }
        if (address.getHouse() != null && !address.getHouse().isBlank()) {
            streetParts.add("д. " + address.getHouse().trim());
        }
        if (address.getBuilding() != null && !address.getBuilding().isBlank()) {
            streetParts.add("к. " + address.getBuilding().trim());
        }
        if (!streetParts.isEmpty()) {
            parts.add(String.join(", ", streetParts));
        }
        return String.join(", ", parts);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuotes = value.contains(";") || value.contains("\"") || value.contains(",")
                || value.contains("\n") || value.contains("\r");
        String normalized = value.replace("\"", "\"\"");
        return needQuotes ? "\"" + normalized + "\"" : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String flatten(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
