package com.example.Kadr.service;

import com.example.Kadr.model.*;
import com.example.Kadr.repository.ActionRepository;
import com.example.Kadr.repository.LogEntryRepository;
import com.example.Kadr.repository.UserLogRepository;
import com.example.Kadr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final LogEntryRepository logEntryRepository;
    private final ActionRepository actionRepository;
    private final UserRepository userRepository;
    private final UserLogRepository userLogRepository;

    @Transactional
    public void log(AuditAction action, String tableName, String comment) {
        if (action == null || tableName == null || tableName.isBlank()) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User actor = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            String actorUsername = authentication.getName();
            actor = userRepository.findByUsernameIgnoreCase(actorUsername).orElse(null);
        }

        String displayTable = tableName.trim();
        String normalizedTable = displayTable.toLowerCase();
        Action actionEntity = resolveActionEntity(action, normalizedTable, displayTable);

        LogEntry entry = LogEntry.builder()
                .action(actionEntity)
                .commentText(comment)
                .createdAt(OffsetDateTime.now())
                .build();
        LogEntry saved = logEntryRepository.save(entry);

        if (actor != null) {
            userLogRepository.save(UserLog.builder()
                    .user(actor)
                    .log(saved)
                    .build());
        }
    }

    private Action resolveActionEntity(AuditAction auditAction, String tableKey, String displayTable) {
        String actionKey = auditAction.name() + ":" + tableKey;
        return actionRepository.findByTitleIgnoreCase(actionKey)
                .orElseGet(() -> actionRepository.save(Action.builder()
                        .title(actionKey)
                        .description(buildDescription(auditAction, displayTable))
                        .build()));
    }

    private String buildDescription(AuditAction auditAction, String tableName) {
        String verb;
        switch (auditAction) {
            case CREATE -> verb = "Создание записи";
            case UPDATE -> verb = "Обновление записи";
            case DELETE -> verb = "Удаление записи";
            default -> verb = auditAction.name();
        }
        return verb + " в таблице " + tableName;
    }
}