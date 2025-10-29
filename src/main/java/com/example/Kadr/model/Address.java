package com.example.Kadr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @NotBlank(message = "Введите страну")
    @Size(min = 2, max = 100, message = "Длина страны должна быть от {min} до {max} символов")
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @NotBlank(message = "Введите город")
    @Size(min = 2, max = 100, message = "Длина города должна быть от {min} до {max} символов")
    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @NotBlank(message = "Введите улицу")
    @Size(min = 2, max = 150, message = "Длина улицы должна быть от {min} до {max} символов")
    @Column(name = "street", nullable = false, length = 150)
    private String street;

    @Size(max = 20, message = "Длина поля 'Дом' не должна превышать {max} символов")
    @Column(name = "house", length = 20)
    private String house;

    @Size(max = 20, message = "Длина поля 'Корпус' не должна превышать {max} символов")
    @Column(name = "building", length = 20)
    private String building;
}
