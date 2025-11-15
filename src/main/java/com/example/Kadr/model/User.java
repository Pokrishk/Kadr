package com.example.Kadr.model;

import com.example.Kadr.config.ValidationGroups;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    public static final String NO_EMOJI_REGEX = "^(?!.*[\\p{So}\\p{Cs}\\p{Co}\\p{Cn}]).*$";

    private static final java.util.regex.Pattern DISALLOWED_SYMBOLS =
            java.util.regex.Pattern.compile("[\\p{So}\\p{Cs}\\p{Co}\\p{Cn}]");

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
    @Pattern(regexp = NO_EMOJI_REGEX,
            message = "Email не должен содержать эмодзи или спецсимволы",
            groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnPersist.class})
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

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    @Transient
    @NotBlank(groups = ValidationGroups.OnRegister.class, message = "Пароль обязателен")
    @Size(min = 8, groups = ValidationGroups.OnRegister.class, message = "Пароль минимум 8 символов")
    @Pattern(regexp = NO_EMOJI_REGEX,
            message = "Пароль не должен содержать эмодзи или спецсимволы",
            groups = ValidationGroups.OnRegister.class)
    private String password;

    @Transient
    @NotBlank(groups = ValidationGroups.OnRegister.class, message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    public static boolean containsDisallowedSymbols(String value) {
        return value != null && DISALLOWED_SYMBOLS.matcher(value).find();
    }
    @AssertTrue(groups = ValidationGroups.OnRegister.class, message = "Пароли не совпадают")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) return false;
        return password.equals(confirmPassword);
    }
}
