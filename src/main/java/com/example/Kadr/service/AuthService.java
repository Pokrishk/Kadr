package com.example.Kadr.service;

import com.example.Kadr.config.ValidationGroups;
import com.example.Kadr.model.Role;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.RoleRepository;
import com.example.Kadr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Validator;

@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final Validator validator;

    public AuthService(UserRepository users, RoleRepository roles, Validator validator) {
        this.users = users;
        this.roles = roles;
        this.validator = validator;
    }

    public boolean usernameExists(String username) { return users.findByUsername(username).isPresent(); }
    public boolean emailExists(String email)       { return users.findByEmail(email).isPresent(); }

    @Transactional
    public User registerEntity(User formUser, String encodedPassword) {
        Role userRole = roles.findByTitleIgnoreCase("User")
                .orElseThrow(() -> new IllegalStateException("Роль 'User' не найдена"));

        User toSave = User.builder()
                .username(formUser.getUsername())
                .email(formUser.getEmail())
                .passwordHash(encodedPassword)
                .role(userRole)
                .build();

        var violations = validator.validate(toSave, ValidationGroups.OnPersist.class);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Данные перед сохранением невалидны: " + violations);
        }

        return users.save(toSave);
    }
}