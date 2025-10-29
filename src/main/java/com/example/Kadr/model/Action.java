package com.example.Kadr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Action {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "title", nullable = false, unique = true, length = 120)
    private String title;

    @NotBlank
    @Size(max = 120)
    @Column(name = "description", nullable = false, length = 120)
    private String description;
}
