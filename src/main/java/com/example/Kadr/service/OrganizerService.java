package com.example.Kadr.service;

import com.example.Kadr.model.Organizer;
import com.example.Kadr.repository.OrganizerRepository;
import com.example.Kadr.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerService {
    private final OrganizerRepository organizerRepository;
    private final RoleRepository roleRepository;

    private static final String ORGANIZER_TITLE = "Organizer";

    private Long getOrganizerRoleId() {
        return roleRepository.findByTitleIgnoreCase(ORGANIZER_TITLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Роль организатора не найдена: " + ORGANIZER_TITLE
                ))
                .getId();
    }

    @Transactional(readOnly = true)
    public Page<Organizer> list(String q, Pageable pageable) {
        Long roleId = getOrganizerRoleId();
        if (q != null && !q.isBlank()) {
            return organizerRepository
                    .findByUser_Role_IdAndOrganizationNameContainingIgnoreCase(
                            roleId, q.trim(), pageable
                    );
        }
        return organizerRepository.findByUser_Role_Id(roleId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Organizer> findAll() {
        return organizerRepository.findAllByUser_Role_Id(getOrganizerRoleId());
    }

    @Transactional(readOnly = true)
    public Organizer getCurrentOrganizerOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Пользователь не аутентифицирован");
        String username = auth.getName();
        return organizerRepository.findByUser_UsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("Вы не зарегистрированы как организатор"));
    }
}
