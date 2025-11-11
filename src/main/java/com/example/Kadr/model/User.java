package com.example.Kadr.model;

import com.example.Kadr.config.ValidationGroups;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @NotBlank(groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnPersist.class})
    @Size(max = 100, groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnPersist.class})
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @NotBlank(groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnPersist.class})
    @Email(groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnPersist.class})
    @Size(max = 320, groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnPersist.class})
    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @NotBlank(groups = ValidationGroups.OnPersist.class)
    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @NotNull(groups = ValidationGroups.OnPersist.class)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Builder.Default
    @Column(name = "theme", nullable = false, length = 20)
    private String theme = "system";

    @Builder.Default
    @Column(name = "date_format", nullable = false, length = 32)
    private String dateFormat = "dd.MM.yyyy";

    @Builder.Default
    @Column(name = "number_format", nullable = false, length = 20)
    private String numberFormat = "comma";

    @Builder.Default
    @Column(name = "page_size", nullable = false)
    private Integer pageSize = 20;

    @Builder.Default
    @Lob
    @Column(name = "saved_filters", nullable = false, columnDefinition = "text")
    private String savedFilters = "[]";

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (theme == null || theme.isBlank()) theme = "system";
        if (dateFormat == null || dateFormat.isBlank()) dateFormat = "dd.MM.yyyy";
        if (numberFormat == null || numberFormat.isBlank()) numberFormat = "comma";
        if (pageSize == null || pageSize < 1) pageSize = 20;
        if (savedFilters == null || savedFilters.isBlank()) savedFilters = "[]";
    }

    @Transient
    @NotBlank(groups = ValidationGroups.OnRegister.class, message = "Пароль обязателен")
    @Size(min = 8, groups = ValidationGroups.OnRegister.class, message = "Пароль минимум 8 символов")
    private String password;

    @Transient
    @NotBlank(groups = ValidationGroups.OnRegister.class, message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    @AssertTrue(groups = ValidationGroups.OnRegister.class, message = "Пароли не совпадают")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) return false;
        return password.equals(confirmPassword);
    }
}
