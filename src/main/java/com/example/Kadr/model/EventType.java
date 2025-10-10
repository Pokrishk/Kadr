package com.example.Kadr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "event_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "title", nullable = false, unique = true, length = 150)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}