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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public ImportResult importFromSql(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        try (InputStream in = inputStream) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            sql = stripBom(sql);
            sql = stripSqlComments(sql);

            List<String> statements = splitStatements(sql);
            if (statements.isEmpty()) {
                throw new IllegalArgumentException("Файл должен содержать оператор INSERT INTO event_types");
            }
            int created = 0;
            int updated = 0;
            int skipped = 0;
            int processed = 0;
            Set<String> seen = new HashSet<>();

            for (String statement : statements) {
                String normalized = statement.toLowerCase(Locale.ROOT).trim();
                if (!normalized.startsWith("insert into event_types")) {
                    throw new IllegalArgumentException("Разрешены только INSERT INTO event_types");
                }

                ParsedInsert insert = parseInsert(statement);
                int titleIndex = findColumnIndex(insert.columns(), "title");
                if (titleIndex < 0) {
                    throw new IllegalArgumentException("В операторе INSERT должен присутствовать столбец title");
                }
                int descriptionIndex = findColumnIndex(insert.columns(), "description");

                for (List<String> row : insert.rows()) {
                    String rawTitle = row.get(titleIndex);
                    if (rawTitle == null) {
                        skipped++;
                        continue;
                    }
                    String title = rawTitle.trim();
                    if (title.isEmpty() || title.length() > 150) {
                        skipped++;
                        continue;
                    }
                    String key = title.toLowerCase(Locale.ROOT);
                    if (!seen.add(key)) {
                        skipped++;
                        continue;
                    }
                    String description = null;
                    if (descriptionIndex >= 0) {
                        String rawDescription = row.get(descriptionIndex);
                        if (rawDescription != null) {
                            description = rawDescription.trim();
                            if (description.isEmpty()) {
                                description = null;
                            }
                        }
                    }
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
                            EventType type = new EventType();
                            type.setTitle(title);
                            type.setDescription(description);
                            eventTypeRepository.save(type);
                            created++;
                            processed++;
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
            throw new IllegalArgumentException("Не удалось прочитать SQL: " + ex.getMessage(), ex);
        }
    }

    public record ImportResult(int processed, int created, int updated, int skipped) { }

    private ParsedInsert parseInsert(String statement) {
        String trimmed = statement.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        Matcher matcher = INSERT_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Не удалось разобрать оператор INSERT INTO event_types");
        }

        List<String> columns = parseColumns(matcher.group(1));
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Не указаны столбцы для вставки");
        }

        String valuesPart = matcher.group(2).trim();
        List<List<String>> rows = parseValueRows(valuesPart, columns.size());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Не найдены значения для вставки");
        }

        return new ParsedInsert(columns, rows);
    }

    private List<String> parseColumns(String columnsRaw) {
        return Arrays.stream(columnsRaw.split(","))
                .map(col -> col.replace("`", "").replace("\"", "").trim())
                .filter(col -> !col.isEmpty())
                .collect(Collectors.toList());
    }

    private List<List<String>> parseValueRows(String valuesPart, int columnCount) {
        List<List<String>> rows = new ArrayList<>();
        int len = valuesPart.length();
        int i = 0;
        while (i < len) {
            while (i < len && Character.isWhitespace(valuesPart.charAt(i))) i++;
            if (i >= len) {
                break;
            }
            if (valuesPart.charAt(i) != '(') {
                throw new IllegalArgumentException("Ожидалась '(' перед списком значений");
            }
            i++;
            List<String> row = new ArrayList<>(columnCount);
            StringBuilder current = new StringBuilder();
            boolean inString = false;
            boolean valueIsString = false;
            while (i < len) {
                char ch = valuesPart.charAt(i);
                if (inString) {
                    if (ch == '\'') {
                        if (i + 1 < len && valuesPart.charAt(i + 1) == '\'') {
                            current.append('\'');
                            i += 2;
                            continue;
                        }
                        inString = false;
                        i++;
                        continue;
                    }
                    current.append(ch);
                    i++;
                    continue;
                }

                if (ch == '\'') {
                    inString = true;
                    valueIsString = true;
                    i++;
                    continue;
                }
                if (ch == ',') {
                    row.add(convertSqlValue(current, valueIsString));
                    current.setLength(0);
                    valueIsString = false;
                    i++;
                    continue;
                }
                if (ch == ')') {
                    row.add(convertSqlValue(current, valueIsString));
                    current.setLength(0);
                    valueIsString = false;
                    i++;
                    break;
                }
                current.append(ch);
                i++;
            }

            if (row.size() != columnCount) {
                throw new IllegalArgumentException("Количество значений не совпадает с количеством столбцов");
            }
            rows.add(row);

            while (i < len && Character.isWhitespace(valuesPart.charAt(i))) i++;
            if (i < len && valuesPart.charAt(i) == ',') {
                i++;
            }
        }
        return rows;
    }

    private String convertSqlValue(CharSequence token, boolean isString) {
        if (isString) {
            return token.toString();
        }
        String raw = token.toString().trim();
        if (raw.isEmpty() || raw.equalsIgnoreCase("null")) {
            return null;
        }
        if (raw.startsWith("\'") && raw.endsWith("\'") && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private int findColumnIndex(List<String> columns, String name) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private String stripBom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private String stripSqlComments(String sql) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        String noBlock = sql.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlock.replaceAll("(?m)^\\s*--.*$", " ");
    }

    private List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        if (sql == null) {
            return statements;
        }
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (inString) {
                if (ch == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                        current.append(ch);
                    }
                } else {
                    current.append(ch);
                }
            } else {
                if (ch == '\'') {
                    inString = true;
                    current.append(ch);
                } else if (ch == ';') {
                    String stmt = current.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }
    private record ParsedInsert(List<String> columns, List<List<String>> rows) { }

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "^insert\\s+into\\s+event_types\\s*\\(([^)]+)\\)\\s*values\\s*(.+)$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
}
