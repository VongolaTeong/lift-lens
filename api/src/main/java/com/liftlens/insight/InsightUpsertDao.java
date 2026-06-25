package com.liftlens.insight;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persists detector output. Upsert is keyed on the {@code uq_insight} natural key so re-runs update
 * in place rather than duplicate; a re-detected RESOLVED insight re-activates, but a DISMISSED one
 * stays dismissed (CLAUDE.md §5).
 */
@Repository
public class InsightUpsertDao {

    private static final String UPSERT = """
            INSERT INTO insight
                (type, exercise_id, muscle, severity, title, detail,
                 window_start, window_end, metric_json, status, detected_at)
            VALUES
                (:type, :exerciseId, :muscle, :severity, :title, :detail,
                 :windowStart, :windowEnd, CAST(:metricJson AS jsonb), 'ACTIVE', now())
            ON CONFLICT ON CONSTRAINT uq_insight DO UPDATE SET
                severity     = EXCLUDED.severity,
                title        = EXCLUDED.title,
                detail       = EXCLUDED.detail,
                window_start = EXCLUDED.window_start,
                metric_json  = EXCLUDED.metric_json,
                detected_at  = now(),
                status       = CASE WHEN insight.status = 'DISMISSED'
                                    THEN 'DISMISSED' ELSE 'ACTIVE' END
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public InsightUpsertDao(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void upsert(DetectedInsight insight) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("type", insight.type().name())
                .addValue("exerciseId", insight.exerciseId(), Types.BIGINT)
                .addValue("muscle", insight.muscle(), Types.VARCHAR)
                .addValue("severity", insight.severity().name())
                .addValue("title", insight.title())
                .addValue("detail", insight.detail())
                .addValue("windowStart", insight.windowStart())
                .addValue("windowEnd", insight.windowEnd())
                .addValue("metricJson", toJson(insight.metric()), Types.VARCHAR);
        jdbc.update(UPSERT, params);
    }

    /** Natural keys of currently-ACTIVE insights for the given types (drives stale-resolve). */
    public List<ActiveInsight> findActiveKeys(Collection<InsightType> types) {
        if (types.isEmpty()) {
            return List.of();
        }
        List<String> typeNames = types.stream().map(InsightType::name).toList();
        return jdbc.query(
                "SELECT id, type, exercise_id, muscle, window_end FROM insight "
                        + "WHERE status = 'ACTIVE' AND type IN (:types)",
                new MapSqlParameterSource("types", typeNames),
                (rs, n) -> new ActiveInsight(
                        rs.getLong("id"),
                        InsightType.valueOf(rs.getString("type")),
                        (Long) rs.getObject("exercise_id"),
                        rs.getString("muscle"),
                        rs.getObject("window_end", LocalDate.class)));
    }

    public void resolve(Collection<Long> insightIds) {
        if (insightIds.isEmpty()) {
            return;
        }
        jdbc.update("UPDATE insight SET status = 'RESOLVED' WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", insightIds));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize insight metric", e);
        }
    }

    public record ActiveInsight(long id, InsightType type, Long exerciseId, String muscle,
            LocalDate windowEnd) {
    }
}
