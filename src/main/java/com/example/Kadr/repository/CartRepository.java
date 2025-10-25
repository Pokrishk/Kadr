package com.example.Kadr.repository;

import com.example.Kadr.model.Cart;
import com.example.Kadr.model.Ticket;
import com.example.Kadr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository <Cart, Long> {
    List<Cart> findByUser(User user);
    Optional<Cart> findByUserAndTicket(User user, Ticket ticket);
    void deleteByUserAndId(User user, Long id);
    @Query("select coalesce(sum(c.quantity), 0) " +
            "from Cart c where c.ticket.event.id = :eventId")
    int sumReservedByEvent(@Param("eventId") Long eventId);
}
