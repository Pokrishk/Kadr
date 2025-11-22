package com.example.Kadr.service;

import com.example.Kadr.model.*;
import com.example.Kadr.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceTest {

    @Mock
    private CartRepository carts;
    @Mock
    private OrderEntityRepository orders;
    @Mock
    private TicketRepository tickets;
    @Mock
    private EventRepository events;

    @InjectMocks
    private CartService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addToCartRejectsPastEvent() {
        Ticket ticket = Ticket.builder()
                .event(Event.builder().eventDatetime(OffsetDateTime.now().minusDays(1)).title("Old").ticketsTotal(10).build())
                .build();
        when(tickets.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.addToCart(new User(), 1L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("прошедшее событие");
    }

    @Test
    void createOrderFromCartCreatesOrderAndClearsCart() {
        Event event = Event.builder().id(2L).ticketsTotal(5).eventDatetime(OffsetDateTime.now().plusDays(1)).title("Show").build();
        Ticket ticket = Ticket.builder().event(event).price(BigDecimal.TEN).build();
        Cart cart = Cart.builder().ticket(ticket).quantity(2).build();
        when(events.findForUpdate(2L)).thenReturn(Optional.of(event));

        OrderEntity order = service.createOrderFromCart(new User(), List.of(cart));

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orders).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getItems()).hasSize(1);
        verify(events).save(event);
        verify(carts).deleteAll(List.of(cart));
        assertThat(order.getTotalSum()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }
}