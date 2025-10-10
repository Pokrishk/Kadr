package com.example.Kadr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @NotBlank
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank
    @Size(max = 150)
    @Column(name = "street", nullable = false, length = 150)
    private String street;

    @Size(max = 20)
    @Column(name = "house", length = 20)
    private String house;

    @Size(max = 20)
    @Column(name = "building", length = 20)
    private String building;
}
