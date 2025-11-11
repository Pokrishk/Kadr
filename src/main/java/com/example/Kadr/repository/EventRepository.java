package com.example.Kadr.repository;

import com.example.Kadr.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
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
    @Override
    @EntityGraph(
            type = EntityGraph.EntityGraphType.FETCH,
            attributePaths = {"eventType", "organizer", "address"}
    )
    List<Event> findAll(org.springframework.data.jpa.domain.Specification<Event> spec, Sort sort);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findForUpdate(@Param("id") Long id);
    Page<Event> findByOrganizer_Id(Long organizerId, Pageable pageable);

    boolean existsByIdAndOrganizer_Id(Long id, Long organizerId);

    @Query("""
        select function('DATE_TRUNC','month', e.eventDatetime) as ym, count(e)
        from Event e
        where e.organizer.id = :orgId
          and e.eventDatetime >= coalesce(:from, e.eventDatetime)
          and e.eventDatetime <= coalesce(:to, e.eventDatetime)
        group by function('DATE_TRUNC','month', e.eventDatetime)
        order by ym
        """)
    List<Object[]> countEventsByMonth(@Param("orgId") Long organizerId,
                                      @Param("from") OffsetDateTime from,
                                      @Param("to") OffsetDateTime to);

    @Query("""
        select et.title, avg(e.rating)
        from Event e join e.eventType et
        where e.organizer.id = :orgId
          and e.eventDatetime >= coalesce(:from, e.eventDatetime)
          and e.eventDatetime <= coalesce(:to, e.eventDatetime)
        group by et.title
        order by et.title
        """)
    List<Object[]> avgRatingByType(@Param("orgId") Long organizerId,
                                   @Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to);
}
