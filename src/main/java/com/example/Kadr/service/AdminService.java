package com.example.Kadr.service;

import com.example.Kadr.config.ValidationGroups;
import com.example.Kadr.model.*;
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
    private final AuditLogService auditLogService;
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
        auditLogService.log(
                AuditAction.UPDATE,
                "users",
                String.format("Назначена роль организатора пользователю %s (ID=%d)",
                        user.getUsername(),
                        user.getId())
        );
    }

    @Transactional
    public void rejectOrganizerRequest(Long organizerId) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        organizerRepository.delete(organizer);
        auditLogService.log(
                AuditAction.DELETE,
                "organizers",
                String.format("Отклонена заявка организатора (ID=%d, пользователь=%s)",
                        organizer.getId(),
                        organizer.getUser().getUsername())
        );
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

        if (isOrganizerRole(role)) {
            throw new IllegalArgumentException("Нельзя назначить роль организатора без заявки");
        }

        User user = new User();
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));

        validateUser(user);
        User saved = userRepository.save(user);
        auditLogService.log(
                AuditAction.CREATE,
                "users",
                String.format("Создан пользователь %s (ID=%d)", saved.getUsername(), saved.getId())
        );
        return saved;
    }

    @Transactional
    public User updateUser(Long id, User form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Role role = getRoleById(form.getRole() != null ? form.getRole().getId() : null);
        if (isOrganizerRole(role) && !organizerRepository.existsByUser_Id(id)) {
            throw new IllegalArgumentException(
                    "Нельзя назначить роль организатора пользователю без заявки организатора"
            );
        }

        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail().trim());
        user.setRole(role);

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }

        validateUser(user);
        User saved = userRepository.save(user);
        auditLogService.log(
                AuditAction.UPDATE,
                "users",
                String.format("Обновлены данные пользователя %s (ID=%d)",
                        saved.getUsername(),
                        saved.getId())
        );
        return saved;
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
        userRepository.deleteById(id);
        auditLogService.log(
                AuditAction.DELETE,
                "users",
                String.format("Удалён пользователь ID=%d", id)
        );
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

    private boolean isOrganizerRole(Role role) {
        return role != null && role.getTitle() != null
                && ORGANIZER_ROLE_TITLE.equalsIgnoreCase(role.getTitle());
    }

    private void validateUser(User user) {
        var violations = validator.validate(user, ValidationGroups.OnPersist.class);
        if (!violations.isEmpty()) {
            ConstraintViolation<User> violation = violations.iterator().next();
            throw new IllegalArgumentException(violation.getMessage());
        }
    }
}