package com.example.Kadr.repository;

import com.example.Kadr.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
}
