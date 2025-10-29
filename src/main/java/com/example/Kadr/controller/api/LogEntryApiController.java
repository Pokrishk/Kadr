package com.example.Kadr.controller.api;

import com.example.Kadr.model.LogEntry;
import com.example.Kadr.repository.LogEntryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Журнал", description = "Работа с журналом действий пользователей")
public class LogEntryApiController extends AbstractCrudApiController<LogEntry> {

    private final LogEntryRepository logEntryRepository;

    public LogEntryApiController(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    @Override
    protected JpaRepository<LogEntry, Long> getRepository() {
        return logEntryRepository;
    }

    @Override
    protected String getResourceName() {
        return "Запись журнала";
    }
}