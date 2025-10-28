package com.example.Kadr.service;

import com.example.Kadr.model.Address;
import com.example.Kadr.model.AuditAction;
import com.example.Kadr.model.Event;
import com.example.Kadr.model.Organizer;
import com.example.Kadr.repository.EventRepository;
import com.example.Kadr.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<Event> getUpcomingRandom(OffsetDateTime from, int poolSize, int limit) {
        var page = eventRepository.findByEventDatetimeAfter(
                from, PageRequest.of(0, Math.max(poolSize, limit), Sort.by("eventDatetime").ascending())
        );

        List<Event> pool = new ArrayList<>(page.getContent());
        Collections.shuffle(pool);
        return pool.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Event> findByIdWithRelations(Long id) {
        return eventRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) { return eventRepository.findById(id); }

    @Transactional
    public Event save(Event e) {
        boolean isNew = e.getId() == null;
        Event saved = eventRepository.save(e);
        auditLogService.log(
                isNew ? AuditAction.CREATE : AuditAction.UPDATE,
                "events",
                String.format("%s событие \"%s\" (ID=%d)",
                        isNew ? "Создано" : "Обновлено",
                        saved.getTitle(),
                        saved.getId())
        );
        return saved;
    }

    @Transactional
    public boolean deleteById(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
            auditLogService.log(
                    AuditAction.DELETE,
                    "events",
                    String.format("Удалено событие ID=%d", id)
            );
            return true;
        }
        return false;
    }
    @Transactional(readOnly = true)
    public Page<Event> search(
            String q,
            Long typeId,
            Long organizerId,
            LocalDate dateFrom,
            LocalDate dateTo,
            LocalTime timeFrom,
            LocalTime timeTo,
            int page,
            int size,
            String sortDir
    ) {
        List<Specification<Event>> specs = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            specs.add((root, cq, cb) -> cb.like(cb.lower(root.get("title")), like));
        }

        if (typeId != null) {
            specs.add((root, cq, cb) -> cb.equal(root.get("eventType").get("id"), typeId));
        }

        if (organizerId != null) {
            specs.add((root, cq, cb) -> cb.equal(root.get("organizer").get("id"), organizerId));
        }

        Optional<OffsetDateTime> fromOdt = composeFrom(dateFrom, timeFrom);
        Optional<OffsetDateTime> toOdt   = composeTo(dateTo, timeTo);

        fromOdt.ifPresent(odt ->
                specs.add((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("eventDatetime"), odt))
        );
        toOdt.ifPresent(odt ->
                specs.add((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("eventDatetime"), odt))
        );

        Specification<Event> spec = specs.isEmpty() ? null : Specification.allOf(specs);

        Sort sort = Sort.by("eventDatetime");
        sort = "desc".equalsIgnoreCase(sortDir) ? sort.descending() : sort.ascending();

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
        return eventRepository.findAll(spec, pageable);
    }

    private Optional<OffsetDateTime> composeFrom(LocalDate d, LocalTime t) {
        if (d == null && t == null) return Optional.empty();
        LocalDate date = d != null ? d : LocalDate.now();
        LocalTime time = t != null ? t : LocalTime.MIN;
        return Optional.of(toOffset(date.atTime(time)));
    }

    private Optional<OffsetDateTime> composeTo(LocalDate d, LocalTime t) {
        if (d == null && t == null) return Optional.empty();
        LocalDate date = d != null ? d : LocalDate.now();
        LocalTime time = t != null ? t : LocalTime.of(23, 59, 59);
        return Optional.of(toOffset(date.atTime(time)));
    }

    private OffsetDateTime toOffset(LocalDateTime ldt) {
        ZoneId zone = ZoneId.systemDefault();
        return ldt.atZone(zone).toOffsetDateTime();
    }

    @Transactional(readOnly = true)
    public Page<Event> listForOrganizer(Long organizerId, int page, int size, String sortDir) {
        Sort sort = Sort.by("eventDatetime");
        sort = "desc".equalsIgnoreCase(sortDir) ? sort.descending() : sort.ascending();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);
        return eventRepository.findByOrganizer_Id(organizerId, pageable);
    }

    @Transactional(readOnly = true)
    public Event getByIdForOrganizerOrThrow(Long id, Long organizerId) {
        return eventRepository.findById(id)
                .filter(e -> e.getOrganizer().getId().equals(organizerId))
                .orElseThrow(() -> new IllegalArgumentException("Событие не найдено или вам не принадлежит"));
    }

    @Transactional
    public Event createForOrganizer(Event e, Organizer organizer) {
        e.setId(null);
        e.setOrganizer(organizer);
        if (e.getRating() == null) e.setRating(java.math.BigDecimal.ZERO);
        Event saved = eventRepository.save(e);
        auditLogService.log(
                AuditAction.CREATE,
                "events",
                String.format("Создано событие \"%s\" (ID=%d)",
                        saved.getTitle(),
                        saved.getId())
        );
        return saved;
    }

    @Transactional
    public Event updateForOrganizer(Long id, Long organizerId, Event patch) {
        Event existing = getByIdForOrganizerOrThrow(id, organizerId);
        existing.setTitle(patch.getTitle());
        existing.setDescription(patch.getDescription());
        existing.setTicketsTotal(patch.getTicketsTotal());
        existing.setEventDatetime(patch.getEventDatetime());
        if (patch.getEventType() != null) existing.setEventType(patch.getEventType());

        if (patch.getAddress() != null) {
            Address a = existing.getAddress();
            if (a == null) a = new Address();
            a.setCountry(patch.getAddress().getCountry());
            a.setCity(patch.getAddress().getCity());
            a.setStreet(patch.getAddress().getStreet());
            a.setHouse(patch.getAddress().getHouse());
            a.setBuilding(patch.getAddress().getBuilding());
            existing.setAddress(a);
        }

        Event saved = eventRepository.save(existing);
        auditLogService.log(
                AuditAction.UPDATE,
                "events",
                String.format("Обновлено событие \"%s\" (ID=%d)",
                        saved.getTitle(),
                        saved.getId())
        );
        return saved;
    }

    @Transactional
    public void deleteForOrganizer(Long id, Long organizerId) {
        Event e = getByIdForOrganizerOrThrow(id, organizerId);
        eventRepository.deleteById(e.getId());
        auditLogService.log(
                AuditAction.DELETE,
                "events",
                String.format("Удалено событие \"%s\" (ID=%d)",
                        e.getTitle(),
                        e.getId())
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> statsEventsByMonth(Long organizerId) {
        var rows = eventRepository.countEventsByMonth(organizerId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Object[] r : rows) {
            res.add(Map.of(
                    "label", formatMonth(r[0]),
                    "count", ((Number) r[1]).longValue()
            ));
        }
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> statsRevenueByMonth(Long organizerId) {
        var rows = ticketRepository.revenueByMonth(organizerId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Object[] r : rows) {
            res.add(Map.of(
                    "label", formatMonth(r[0]),
                    "revenue", ((Number) r[1]).doubleValue()
            ));
        }
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> statsTicketsAndRevenueByEvent(Long organizerId) {
        var rows = ticketRepository.ticketsAndRevenueByEvent(organizerId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Object[] r : rows) {
            res.add(Map.of(
                    "eventId", (Long) r[0],
                    "title", (String) r[1],
                    "tickets", ((Number) r[2]).longValue(),
                    "revenue", ((Number) r[3]).doubleValue()
            ));
        }
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> statsAvgRatingByType(Long organizerId) {
        var rows = eventRepository.avgRatingByType(organizerId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Object[] r : rows) {
            res.add(Map.of(
                    "type", (String) r[0],
                    "avgRating", ((Number) r[1]).doubleValue()
            ));
        }
        return res;
    }

    private String formatMonth(Object dbVal) {
        if (dbVal instanceof OffsetDateTime odt) {
            return odt.getYear() + "-" + String.format("%02d", odt.getMonthValue());
        }
        if (dbVal instanceof LocalDateTime ldt) {
            return ldt.getYear() + "-" + String.format("%02d", ldt.getMonthValue());
        }
        return String.valueOf(dbVal);
    }
}
