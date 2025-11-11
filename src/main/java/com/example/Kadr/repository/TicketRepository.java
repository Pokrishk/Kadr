package com.example.Kadr.repository;

import com.example.Kadr.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEvent_Id(Long eventId);

    @Query("""
        select e.id, e.title, count(t), coalesce(sum(t.price), 0)
        from Ticket t join t.event e
        where e.organizer.id = :orgId
          and e.eventDatetime >= coalesce(:from, e.eventDatetime)
          and e.eventDatetime <= coalesce(:to, e.eventDatetime)
        group by e.id, e.title
        order by e.eventDatetime desc
        """)
    List<Object[]> ticketsAndRevenueByEvent(@Param("orgId") Long organizerId,
                                            @Param("from") OffsetDateTime from,
                                            @Param("to") OffsetDateTime to);

    @Query("""
        select function('DATE_TRUNC','month', e.eventDatetime) as ym, coalesce(sum(t.price),0)
        from Ticket t join t.event e
        where e.organizer.id = :orgId
          and e.eventDatetime >= coalesce(:from, e.eventDatetime)
          and e.eventDatetime <= coalesce(:to, e.eventDatetime)
        group by function('DATE_TRUNC','month', e.eventDatetime)
        order by ym
        """)
    List<Object[]> revenueByMonth(@Param("orgId") Long organizerId,
                                  @Param("from") OffsetDateTime from,
                                  @Param("to") OffsetDateTime to);
}
