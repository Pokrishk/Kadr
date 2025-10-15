package com.example.Kadr.service;

import com.example.Kadr.model.Organizer;
import com.example.Kadr.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerService {
    private final OrganizerRepository organizerRepository;

    @Transactional(readOnly = true)
    public Page<Organizer> list(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return organizerRepository.findByOrganizationNameContainingIgnoreCase(q.trim(), pageable);
        }
        return organizerRepository.findAll(pageable);
    }
    @Transactional(readOnly = true)
    public List<Organizer> findAll() { return organizerRepository.findAll(); }
}
