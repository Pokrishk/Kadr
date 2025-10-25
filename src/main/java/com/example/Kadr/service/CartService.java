package com.example.Kadr.service;

import com.example.Kadr.model.*;
import com.example.Kadr.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {
    private final CartRepository carts;
    private final OrderEntityRepository orders;
    private final TicketRepository tickets;
    private final EventRepository events;

    public CartService(CartRepository carts,
                       OrderEntityRepository orders,
                       TicketRepository tickets,
                       EventRepository events) {
        this.carts = carts;
        this.orders = orders;
        this.tickets = tickets;
        this.events = events;
    }

    public List<Cart> getUserCart(User user) {
        return carts.findByUser(user);
    }

    @Transactional
    public void deleteItem(User user, Long cartId) {
        carts.deleteByUserAndId(user, cartId);
    }

    @Transactional
    public OrderEntity createOrderFromCart(User user, List<Cart> items) {
        items.forEach(cart -> {
            if (cart.getTicket().getEvent().getEventDatetime().isBefore(OffsetDateTime.now())) {
                throw new IllegalArgumentException("Нельзя заказать прошедшее событие: " +
                        cart.getTicket().getEvent().getTitle());
            }
        });
        BigDecimal total = items.stream()
                .map(i -> i.getTicket().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Integer> needByEvent = items.stream().collect(Collectors.toMap(
                it -> it.getTicket().getEvent().getId(),
                Cart::getQuantity,
                Integer::sum
        ));
        for (Map.Entry<Long, Integer> e : needByEvent.entrySet()) {
            Long eventId = e.getKey();
            int need = e.getValue();

            Event ev = events.findForUpdate(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Событие не найдено: " + eventId));

            if (ev.getEventDatetime().isBefore(OffsetDateTime.now())) {
                throw new IllegalArgumentException("Событие уже прошло: " + ev.getTitle());
            }

            int left = ev.getTicketsTotal() == null ? 0 : ev.getTicketsTotal();
            if (need > left) {
                throw new IllegalArgumentException("Недостаточно билетов на \"" + ev.getTitle() +
                        "\". Осталось: " + left + ", нужно: " + need + ".");
            }

            ev.setTicketsTotal(left - need);
            events.save(ev);
        }
        OrderEntity order = OrderEntity.builder()
                .user(user)
                .createdAt(OffsetDateTime.now())
                .totalSum(total)
                .items(new ArrayList<>())
                .build();

        items.forEach(cart -> {
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .ticket(cart.getTicket())
                    .quantity(cart.getQuantity())
                    .build();
            order.getItems().add(oi);
        });
        orders.save(order);
        carts.deleteAll(items);
        return order;
    }

    @Transactional
    public void addToCart(User user, Long ticketId, int quantity) {
        if (quantity < 1) quantity = 1;
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Билет не найден"));
        Event event = ticket.getEvent();
        if (event.getEventDatetime().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Нельзя добавить билет на прошедшее событие");
        }
        int reservedTotal = carts.sumReservedByEvent(event.getId());
        var existingOpt = carts.findByUserAndTicket(user, ticket);
        int userExistingQty = existingOpt.map(Cart::getQuantity).orElse(0);
        int leftEvent = event.getTicketsTotal() == null ? 0 : event.getTicketsTotal();
        int availableForUser = leftEvent - (reservedTotal - userExistingQty);
        if (availableForUser <= 0) {
            throw new IllegalArgumentException("Билеты на \"" + event.getTitle() + "\" закончились.");
        }
        int desiredForThisTicket = userExistingQty + quantity;

        if (desiredForThisTicket > availableForUser) {
            throw new IllegalArgumentException("Нельзя добавить больше доступного. Доступно: " +
                    availableForUser + " шт.");
        }

        if (existingOpt.isPresent()) {
            Cart item = existingOpt.get();
            item.setQuantity(desiredForThisTicket);
        } else {
            Cart item = Cart.builder()
                    .user(user)
                    .ticket(ticket)
                    .quantity(quantity)
                    .build();
            carts.save(item);
        }
    }
}
