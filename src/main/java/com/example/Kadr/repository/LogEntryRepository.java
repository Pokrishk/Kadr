package com.example.Kadr.repository;

import com.example.Kadr.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    @EntityGraph(attributePaths = {"action"})
    Page<LogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"action"})
    List<LogEntry> findAllByOrderByCreatedAtDesc();
}
