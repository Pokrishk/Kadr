package com.example.Kadr.controller;

import com.example.Kadr.config.ValidationGroups;
import com.example.Kadr.model.*;
import com.example.Kadr.repository.AddressRepository;
import com.example.Kadr.repository.EventRepository;
import com.example.Kadr.repository.EventTypeRepository;
import com.example.Kadr.repository.TicketRepository;
import com.example.Kadr.service.EventService;
import com.example.Kadr.service.OrganizerService;
import com.example.Kadr.service.UserSettingsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
@RequiredArgsConstructor
@RequestMapping("/organizer")
public class OrganizerController {
    private final OrganizerService organizerService;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final AddressRepository addressRepository;
    private final EventTypeRepository eventTypeRepository;
    private final TicketRepository ticketRepository;
    private final UserSettingsService userSettingsService;


    @GetMapping("/panel")
    public String panel(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(value = "size", required = false) Integer size,
                        @RequestParam(defaultValue = "desc") String sort,
                        @AuthenticationPrincipal UserDetails principal,
                        Model model) {

        Organizer org = organizerService.getCurrentOrganizerOrThrow();
        var userSettings = principal == null ? null
                : userSettingsService.ensureSettingsForUsername(principal.getUsername());
        int effectiveSize = (size != null && size > 0) ? size
                : userSettings != null ? userSettings.getPageSize() : 20;
        Page<com.example.Kadr.model.Event> events =
                eventService.listForOrganizer(org.getId(), page, effectiveSize, sort);

        model.addAttribute("organizer", org);
        model.addAttribute("events", events);
        model.addAttribute("page", page);
        model.addAttribute("size", effectiveSize);
        model.addAttribute("sort", sort);
        return "organizer-panel";
    }

    @GetMapping("/stats")
    public String stats(@RequestParam(value = "from", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                        LocalDate from,
                        @RequestParam(value = "to", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                        LocalDate to,
                        @AuthenticationPrincipal UserDetails principal,
                        HttpServletRequest request,
                        Model model) {
        Organizer org = organizerService.getCurrentOrganizerOrThrow();
        Long orgId = org.getId();

        if (principal != null) {
            var settings = userSettingsService.ensureSettingsForUsername(principal.getUsername());
            var params = request.getParameterMap();
            var saved = userSettingsService.getSavedFilters(settings, "organizer_stats");
            if (saved != null) {
                if (!params.containsKey("from") && saved.hasNonNull("from")) {
                    from = parseDate(saved.get("from").asText());
                }
                if (!params.containsKey("to") && saved.hasNonNull("to")) {
                    to = parseDate(saved.get("to").asText());
                }
            }
        }

        var range = normalizeRange(from, to);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        if (range.error() != null) {
            model.addAttribute("filterError", range.error());
        }
        model.addAttribute("statsEventsByMonth",             eventService.statsEventsByMonth(orgId, range.from(), range.to()));
        model.addAttribute("statsRevenueByMonth",            eventService.statsRevenueByMonth(orgId, range.from(), range.to()));
        model.addAttribute("statsTicketsAndRevenueByEvent",  eventService.statsTicketsAndRevenueByEvent(orgId, range.from(), range.to()));
        model.addAttribute("statsAvgRatingByType",           eventService.statsAvgRatingByType(orgId, range.from(), range.to()));

        return "organizer-stats";
    }
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    @GetMapping("/events/new")
    public String newEventForm(Model model) {
        Organizer org = organizerService.getCurrentOrganizerOrThrow();

        Event e = new Event();
        e.setAddress(new Address());
        e.setEventType(new EventType());
        e.setEventDateTimeLocal(LocalDateTime.now().withSecond(0).withNano(0));
        e.getTickets().add(Ticket.builder().price(new BigDecimal("0.00")).build());

        model.addAttribute("organizer", org);
        model.addAttribute("types", eventTypeRepository.findAll());
        model.addAttribute("event", e);
        model.addAttribute("mode", "create");
        return "organizer-event-form";
    }

    @PostMapping("/events")
    public String createEvent(@ModelAttribute("event") @Validated(ValidationGroups.FormGroup.class) Event form,
                              org.springframework.validation.BindingResult br,
                              Model model,
                              RedirectAttributes ra) {
        if (form.getEventType() == null || form.getEventType().getId() == null) {
            br.rejectValue("eventType.id", "NotNull", "Выберите тип события");
        }
        if (form.getTickets() == null || form.getTickets().stream().noneMatch(t -> t.getPrice() != null)) {
            br.rejectValue("tickets", "NotEmpty", "Нужно добавить хотя бы один тип билета");
        }
        if (br.hasErrors()) {
            Organizer org = organizerService.getCurrentOrganizerOrThrow();
            model.addAttribute("organizer", org);
            model.addAttribute("types", eventTypeRepository.findAll());
            model.addAttribute("mode", "create");
            return "organizer-event-form";
        }
        var organizer = organizerService.getCurrentOrganizerOrThrow();
        var eventType = eventTypeRepository.findById(form.getEventType().getId()).orElseThrow();
        var e = new Event();
        e.setTitle(form.getTitle());
        e.setDescription(form.getDescription());
        e.setTicketsTotal(form.getTicketsTotal());
        e.setAddress(form.getAddress());
        e.setOrganizer(organizer);
        e.setEventType(eventType);
        e.setEventDatetime(form.getEventDateTimeLocal()
                .atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
        if (form.getTickets() != null) {
            for (Ticket t : form.getTickets()) {
                if (t.getPrice() == null) continue;
                e.addTicket(Ticket.builder()
                        .price(t.getPrice())
                        .seat(t.getSeat() == null || t.getSeat().isBlank() ? null : t.getSeat().trim())
                        .build());
            }
        }
        eventService.save(e);
        ra.addFlashAttribute("notice", "Событие создано.");
        return "redirect:/organizer/panel";
    }

    @GetMapping("/events/{id}/edit")
    public String editEvent(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Organizer org = organizerService.getCurrentOrganizerOrThrow();
        Event e = eventService.getByIdForOrganizerOrThrow(id, org.getId());

        e.setEventDateTimeLocal(e.getEventDatetime()
                .atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime());

        if (e.getAddress()==null) e.setAddress(new Address());
        if (e.getTickets() == null || e.getTickets().isEmpty()) {
            e.getTickets().add(
                    Ticket.builder().price(new BigDecimal("0.00")).seat(null).build()
            );
        }

        model.addAttribute("organizer", org);
        model.addAttribute("types", eventTypeRepository.findAll());
        model.addAttribute("event", e);
        model.addAttribute("mode", "edit");
        model.addAttribute("eventId", id);
        return "organizer-event-form";
    }

    @PostMapping("/events/{id}")
    public String updateEvent(@PathVariable Long id,
                              @ModelAttribute("event") @Validated(ValidationGroups.FormGroup.class) Event form,
                              org.springframework.validation.BindingResult br,
                              Model model,
                              RedirectAttributes ra) {

        if (form.getEventType() == null || form.getEventType().getId() == null) {
            br.rejectValue("eventType.id", "NotNull", "Выберите тип события");
        }
        if (form.getTickets() == null || form.getTickets().stream().noneMatch(t -> t.getPrice() != null)) {
            br.rejectValue("tickets", "NotEmpty", "Нужно оставить хотя бы один тип билета");
        }
        if (br.hasErrors()) {
            Organizer org = organizerService.getCurrentOrganizerOrThrow();
            model.addAttribute("organizer", org);
            model.addAttribute("types", eventTypeRepository.findAll());
            model.addAttribute("mode", "edit");
            model.addAttribute("eventId", id);
            return "organizer-event-form";
        }
        Organizer org = organizerService.getCurrentOrganizerOrThrow();
        Event e = eventService.getByIdForOrganizerOrThrow(id, org.getId());
        var type = eventTypeRepository.findById(form.getEventType().getId()).orElseThrow();
        e.setTitle(form.getTitle());
        e.setDescription(form.getDescription());
        e.setTicketsTotal(form.getTicketsTotal());
        e.setEventType(type);
        e.setEventDatetime(form.getEventDateTimeLocal()
                .atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime());
        Address a = e.getAddress();
        if (a == null) a = new Address();
        a.setCountry(form.getAddress().getCountry());
        a.setCity(form.getAddress().getCity());
        a.setStreet(form.getAddress().getStreet());
        a.setHouse(form.getAddress().getHouse());
        a.setBuilding(form.getAddress().getBuilding());
        e.setAddress(a);
        e.getTickets().clear();
        if (form.getTickets() != null) {
            for (Ticket t : form.getTickets()) {
                if (t.getPrice() == null) continue;
                e.addTicket(Ticket.builder()
                        .price(t.getPrice())
                        .seat(t.getSeat() == null || t.getSeat().isBlank() ? null : t.getSeat().trim())
                        .build());
            }
        }
        eventService.save(e);
        ra.addFlashAttribute("notice", "Событие обновлено");
        return "redirect:/organizer/panel";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes ra) {
        Organizer org = organizerService.getCurrentOrganizerOrThrow();
        try {
            eventService.deleteForOrganizer(id, org.getId());
            ra.addFlashAttribute("notice", "Событие удалено");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/organizer/panel";
    }

    private java.time.OffsetDateTime toOffset(java.time.LocalDateTime ldt) {
        if (ldt == null) throw new IllegalArgumentException("Укажите дату и время");
        return ldt.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
    }

    @GetMapping("/stats/export")
    public void exportStats(@RequestParam(value = "from", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate from,
                            @RequestParam(value = "to", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                            LocalDate to,
                            HttpServletResponse response) throws IOException {
        Organizer org = organizerService.getCurrentOrganizerOrThrow();
        Long orgId = org.getId();

        var range = normalizeRange(from, to);

        var eventsByMonth   = eventService.statsEventsByMonth(orgId, range.from(), range.to());
        var revenueByMonth  = eventService.statsRevenueByMonth(orgId, range.from(), range.to());
        var ticketsByEvent  = eventService.statsTicketsAndRevenueByEvent(orgId, range.from(), range.to());
        var ratingByType    = eventService.statsAvgRatingByType(orgId, range.from(), range.to());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Статистика");
            int rowNum = 0;
            Row r1 = sheet.createRow(rowNum++);
            r1.createCell(0).setCellValue("События по месяцам");
            rowNum++;
            Row header1 = sheet.createRow(rowNum++);
            header1.createCell(0).setCellValue("Месяц");
            header1.createCell(1).setCellValue("Количество");
            for (var row : eventsByMonth) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.get("label").toString());
                r.createCell(1).setCellValue(((Number) row.get("count")).doubleValue());
            }
            rowNum += 2;
            Row r2 = sheet.createRow(rowNum++);
            r2.createCell(0).setCellValue("Выручка по месяцам (₽)");
            rowNum++;
            Row header2 = sheet.createRow(rowNum++);
            header2.createCell(0).setCellValue("Месяц");
            header2.createCell(1).setCellValue("Выручка (₽)");
            for (var row : revenueByMonth) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.get("label").toString());
                r.createCell(1).setCellValue(((Number) row.get("revenue")).doubleValue());
            }
            rowNum += 2;
            Row r3 = sheet.createRow(rowNum++);
            r3.createCell(0).setCellValue("Средний рейтинг по типам");
            rowNum++;
            Row header3 = sheet.createRow(rowNum++);
            header3.createCell(0).setCellValue("Тип события");
            header3.createCell(1).setCellValue("Средний рейтинг");
            for (var row : ratingByType) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.get("type").toString());
                r.createCell(1).setCellValue(((Number) row.get("avgRating")).doubleValue());
            }
            rowNum += 2;
            Row r4 = sheet.createRow(rowNum++);
            r4.createCell(0).setCellValue("Билеты и выручка по событиям");
            rowNum++;
            Row header4 = sheet.createRow(rowNum++);
            header4.createCell(0).setCellValue("Событие");
            header4.createCell(1).setCellValue("Билеты (шт.)");
            header4.createCell(2).setCellValue("Выручка (₽)");
            for (var row : ticketsByEvent) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(row.get("title").toString());
                r.createCell(1).setCellValue(((Number) row.get("tickets")).doubleValue());
                r.createCell(2).setCellValue(((Number) row.get("revenue")).doubleValue());
            }
            for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = "statistics_" + org.getOrganizationName().replaceAll("\\s+", "_") + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            wb.write(response.getOutputStream());
        }
    }
    private Range normalizeRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            return new Range(null, null, "Дата начала не может быть позже даты окончания");
        }
        return new Range(from, to, null);
    }

    private record Range(LocalDate from, LocalDate to, String error) { }
}
