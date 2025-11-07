package com.example.Kadr.service;

import com.example.Kadr.model.EventType;
import com.example.Kadr.model.AuditAction;
import com.example.Kadr.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventTypeService {

    private final EventTypeRepository eventTypeRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<EventType> getRandomTypes(int poolSize, int limit) {
        var page = eventTypeRepository.findAll(
                PageRequest.of(0, Math.max(poolSize, limit), Sort.by("id").descending())
        );

        List<EventType> pool = new ArrayList<>(page.getContent());
        Collections.shuffle(pool);
        return pool.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventType> findAll() { return eventTypeRepository.findAll(); }

    @Transactional
    public ImportResult importFromCsv(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            boolean firstLine = true;
            boolean headerChecked = false;
            int created = 0;
            int updated = 0;
            int skipped = 0;
            int processed = 0;
            Set<String> seen = new HashSet<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    line = removeBom(line);
                    firstLine = false;
                }
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("sep=")) {
                    continue;
                }

                List<String> cells = splitCsvLine(trimmed);
                if (cells.isEmpty()) {
                    skipped++;
                    continue;
                }

                if (!headerChecked) {
                    headerChecked = true;
                    if (isHeaderRow(cells)) {
                        continue;
                    }
                }

                String rawTitle = cells.get(0) != null ? cells.get(0).trim() : "";
                if (rawTitle.isBlank()) {
                    skipped++;
                    continue;
                }
                if (rawTitle.length() > 150) {
                    skipped++;
                    continue;
                }

                String normalizedKey = rawTitle.toLowerCase(Locale.ROOT);
                if (!seen.add(normalizedKey)) {
                    skipped++;
                    continue;
                }

                String description = cells.size() > 1
                        ? cells.stream()
                        .skip(1)
                        .map(val -> val == null ? "" : val.trim())
                        .filter(val -> !val.isEmpty())
                        .collect(Collectors.joining(" "))
                        : null;
                if (description != null && description.isBlank()) {
                    description = null;
                }

                String title = rawTitle.trim();
                var existingOpt = eventTypeRepository.findByTitleIgnoreCase(title);
                if (existingOpt.isPresent()) {
                    EventType existing = existingOpt.get();
                    boolean changed = !Objects.equals(existing.getDescription(), description)
                            || !Objects.equals(existing.getTitle(), title);
                    if (changed) {
                        existing.setTitle(title);
                        existing.setDescription(description);
                        eventTypeRepository.save(existing);
                        updated++;
                        processed++;
                    } else {
                        skipped++;
                    }
                } else {
                    EventType type = new EventType();
                    type.setTitle(title);
                    type.setDescription(description);
                    eventTypeRepository.save(type);
                    created++;
                    processed++;
                }
            }

            auditLogService.log(
                    AuditAction.CREATE,
                    "event_types",
                    String.format(
                            "Импорт типов событий: обработано %d, создано %d, обновлено %d, пропущено %d",
                            processed, created, updated, skipped
                    )
            );

            return new ImportResult(processed, created, updated, skipped);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Не удалось прочитать CSV: " + ex.getMessage(), ex);
        }
    }

    public record ImportResult(int processed, int created, int updated, int skipped) { }

    private String removeBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private boolean isHeaderRow(List<String> cells) {
        if (cells.isEmpty()) {
            return false;
        }
        String first = cells.get(0) != null ? cells.get(0).trim().toLowerCase(Locale.ROOT) : "";
        return first.equals("title") || first.equals("название");
    }

    private List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        char delimiter = line.indexOf(';') >= 0 ? ';' : ',';
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes && ch == delimiter) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }
}
