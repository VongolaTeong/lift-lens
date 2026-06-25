package com.liftlens.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftlens.web.dto.InsightDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read/dismiss side for the insight feed (CLAUDE.md §7). Insights are returned severity-first then
 * newest-first; {@code metric_json} is surfaced as structured JSON, and the related exercise name is
 * joined in for display.
 */
@Service
public class InsightQueryService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "DISMISSED", "RESOLVED");

    private static final String SELECT = """
            SELECT i.id, i.type, i.severity, i.exercise_id, e.canonical_name AS exercise_name,
                   i.muscle, i.title, i.detail, i.window_start, i.window_end, i.metric_json,
                   i.detected_at, i.status
            FROM insight i
            LEFT JOIN exercise e ON e.id = i.exercise_id
            WHERE (:status IS NULL OR i.status = :status)
            ORDER BY CASE i.severity WHEN 'HIGH' THEN 0 WHEN 'WARN' THEN 1 ELSE 2 END,
                     i.detected_at DESC, i.id DESC
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public InsightQueryService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * Insights filtered by {@code status} ({@code active}/{@code dismissed}/{@code resolved}); {@code all}
     * (or blank) returns every status. An unknown value is rejected with {@link IllegalArgumentException}.
     */
    public List<InsightDto> list(String status) {
        String filter = normalizeStatus(status);
        return jdbc.query(SELECT, new MapSqlParameterSource("status", filter), (rs, n) -> new InsightDto(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("severity"),
                (Long) rs.getObject("exercise_id"),
                rs.getString("exercise_name"),
                rs.getString("muscle"),
                rs.getString("title"),
                rs.getString("detail"),
                rs.getObject("window_start", LocalDate.class),
                rs.getObject("window_end", LocalDate.class),
                parseJson(rs.getString("metric_json")),
                rs.getTimestamp("detected_at") == null ? null : rs.getTimestamp("detected_at").toInstant(),
                rs.getString("status")));
    }

    /** Dismiss an active/resolved insight; 404 if no such id. Idempotent — re-dismissing is a no-op. */
    public void dismiss(long insightId) {
        int updated = jdbc.getJdbcOperations().update(
                "UPDATE insight SET status = 'DISMISSED' WHERE id = ?", insightId);
        if (updated == 0) {
            throw new NoSuchElementException("Insight " + insightId + " not found");
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            return null;
        }
        String upper = status.toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(upper)) {
            throw new IllegalArgumentException(
                    "Unknown status '" + status + "'; expected one of active, dismissed, resolved, all");
        }
        return upper;
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
