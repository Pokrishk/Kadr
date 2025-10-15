package com.example.Kadr.service;

import com.example.Kadr.model.Event;
import com.example.Kadr.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

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
    public Event save(Event e) { return eventRepository.save(e); }

    @Transactional
    public boolean deleteById(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
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
}
