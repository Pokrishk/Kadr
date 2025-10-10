package com.example.Kadr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events",
        indexes = {
                @Index(name = "idx_events_type", columnList = "event_type_id"),
                @Index(name = "idx_events_organizer", columnList = "organizer_id"),
                @Index(name = "idx_events_datetime", columnList = "event_datetime")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank
    @Size(max = 200)
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    @Digits(integer = 1, fraction = 1)
    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @Min(0)
    @Column(name = "tickets_total", nullable = false)
    private Integer ticketsTotal;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @NotNull
    @Column(name = "event_datetime", nullable = false)
    private OffsetDateTime eventDatetime;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id", nullable = false)
    private EventType eventType;
}
