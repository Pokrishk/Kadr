package com.example.Kadr.service;

import com.example.Kadr.config.ValidationGroups;
import com.example.Kadr.model.LogEntry;
import com.example.Kadr.model.Organizer;
import com.example.Kadr.model.Role;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.LogEntryRepository;
import com.example.Kadr.repository.OrganizerRepository;
import com.example.Kadr.repository.RoleRepository;
import com.example.Kadr.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final String ORGANIZER_ROLE_TITLE = "Organizer";

    private final OrganizerRepository organizerRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final LogEntryRepository logEntryRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    @Transactional(readOnly = true)
    public Page<Organizer> getPendingOrganizerRequests(Pageable pageable) {
        return organizerRepository.findPendingRequests(ORGANIZER_ROLE_TITLE, pageable);
    }

    @Transactional
    public void approveOrganizerRequest(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        User user = organizer.getUser();
        Role organizerRole = getOrganizerRole();

        if (user.getRole() != null && organizerRole.getId().equals(user.getRole().getId())) {
            return;
        }

        user.setRole(organizerRole);
        userRepository.save(user);
    }

    @Transactional
    public void rejectOrganizerRequest(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        organizerRepository.delete(organizer);
    }

    @Transactional(readOnly = true)
    public long countPendingOrganizerRequests() {
        return organizerRepository.countPendingRequests(ORGANIZER_ROLE_TITLE);
    }

    @Transactional(readOnly = true)
    public Page<User> findUsers(String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            String normalized = query.trim();
            return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    normalized, normalized, pageable
            );
        }
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username, Long excludeId) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Optional<User> existing = userRepository.findByUsernameIgnoreCase(username.trim());
        return existing.filter(user -> excludeId == null || !user.getId().equals(excludeId)).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email, Long excludeId) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email.trim());
        return existing.filter(user -> excludeId == null || !user.getId().equals(excludeId)).isPresent();
    }

    @Transactional
    public User createUser(User form) {
        Long roleId = form.getRole() != null ? form.getRole().getId() : null;
        Role role = getRoleById(roleId);

        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));

        validateUser(user);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(getRoleById(form.getRole() != null ? form.getRole().getId() : null));

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }

        validateUser(user);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countLogs() {
        return logEntryRepository.count();
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> getLogs(Pageable pageable) {
        return logEntryRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<LogEntry> getLogsForExport() {
        return logEntryRepository.findAllByOrderByCreatedAtDesc();
    }

    private Role getOrganizerRole() {
        return roleRepository.findByTitleIgnoreCase(ORGANIZER_ROLE_TITLE)
                .orElseThrow(() -> new IllegalStateException("Роль организатора не найдена"));
    }

    private Role getRoleById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Роль не найдена");
        }
        return roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Роль не найдена"));
    }

    private void validateUser(User user) {
        var violations = validator.validate(user, ValidationGroups.OnPersist.class);
        if (!violations.isEmpty()) {
            ConstraintViolation<User> violation = violations.iterator().next();
            throw new IllegalArgumentException(violation.getMessage());
        }
    }
}