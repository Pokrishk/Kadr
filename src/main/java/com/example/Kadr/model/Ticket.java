package com.example.Kadr.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_tickets_event", columnList = "event_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @DecimalMin(value = "0.00", inclusive = true, message = "Цена не может быть отрицательной")
    @Digits(integer = 10, fraction = 2, message = "Цена должна быть числом с двумя знаками после запятой")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Size(max = 50, message = "Поле 'Место/тип' не должно превышать 50 символов")
    @Column(name = "seat", length = 50)
    private String seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-tickets")
    private Event event;
}
