package com.example.Kadr.repository;

import com.example.Kadr.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    @EntityGraph(attributePaths = {"eventType", "organizer", "address"})
    Page<Event> findByEventDatetimeAfter(OffsetDateTime from, Pageable pageable);
    @EntityGraph(attributePaths = {"eventType", "address", "organizer"})
    Optional<Event> findById(Long id);
    @Override
    @EntityGraph(
            type = EntityGraph.EntityGraphType.FETCH,
            attributePaths = {"eventType", "organizer", "address"}
    )
    Page<Event> findAll(org.springframework.data.jpa.domain.Specification<Event> spec, Pageable pageable);
}
