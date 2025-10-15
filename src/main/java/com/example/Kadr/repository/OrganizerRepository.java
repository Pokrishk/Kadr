package com.example.Kadr.repository;

import com.example.Kadr.model.Organizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
    @EntityGraph(attributePaths = {"user"})
    Page<Organizer> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Organizer> findByOrganizationNameContainingIgnoreCase(String q, Pageable pageable);
}
