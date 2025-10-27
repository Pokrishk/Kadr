package com.example.Kadr.model;

import com.example.Kadr.config.ValidationGroups;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @NotBlank(message = "Введите название события")
    @Size(max = 200, message = "Название не должно превышать {max} символов")
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank(message = "Введите описание события")
    @Size(max = 200, message = "Описание не должно превышать {max} символов")
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @DecimalMin(value = "0.0", message = "Рейтинг не может быть меньше {value}")
    @DecimalMax(value = "5.0", message = "Рейтинг не может быть больше {value}")
    @Digits(integer = 1, fraction = 1, message = "Рейтинг должен быть числом с одним знаком после запятой")
    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @NotNull(message = "Укажите количество билетов")
    @Min(value = 0, message = "Количество билетов не может быть отрицательным")
    @Column(name = "tickets_total", nullable = false)
    private Integer ticketsTotal;

    @NotNull(message = "Укажите адрес проведения")
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", nullable = false)
    @Valid
    private Address address;

    @NotNull(groups = ValidationGroups.PersistGroup.class)
    @Column(name="event_datetime", nullable=false)
    private OffsetDateTime eventDatetime;

    @NotNull(message = "Организатор обязателен")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @NotNull(message = "Выберите тип события")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id", nullable = false)
    private EventType eventType;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Valid
    private List<Ticket> tickets = new ArrayList<>();

    @Transient
    @NotNull(message = "Укажите дату и время проведения", groups = ValidationGroups.FormGroup.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private java.time.LocalDateTime eventDateTimeLocal;

    public void setTickets(List<Ticket> tickets) {
        this.tickets.clear();
        if (tickets != null) {
            for (Ticket t : tickets) addTicket(t);
        }
    }

    public void addTicket(Ticket t) {
        if (t == null) return;
        t.setEvent(this);
        this.tickets.add(t);
    }

    public void removeTicket(int idx) {
        if (idx >= 0 && idx < tickets.size()) this.tickets.remove(idx);
    }
}
