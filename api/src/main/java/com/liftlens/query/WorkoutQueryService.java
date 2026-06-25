package com.liftlens.query;

import com.liftlens.web.dto.WorkoutSummaryDto;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read side for the session list ({@code GET /api/workouts?from=&to=}). {@code from}/{@code to} are
 * optional UTC-date bounds on the session start; counts and volume aggregate the working sets.
 */
@Service
public class WorkoutQueryService {

    private static final String SELECT = """
            SELECT w.id, w.title, w.started_at, w.ended_at, w.duration_seconds, w.split_category,
                   COUNT(DISTINCT es.exercise_id) FILTER (WHERE es.is_working) AS exercise_count,
                   COUNT(es.id) FILTER (WHERE es.is_working) AS set_count,
                   COALESCE(SUM(CASE WHEN es.is_working AND es.weight_kg IS NOT NULL AND es.reps IS NOT NULL
                                THEN es.weight_kg * es.reps ELSE 0 END), 0) AS working_volume
            FROM workout w
            LEFT JOIN exercise_set es ON es.workout_id = w.id
            WHERE (CAST(:from AS date) IS NULL OR (w.started_at AT TIME ZONE 'UTC')::date >= :from)
              AND (CAST(:to   AS date) IS NULL OR (w.started_at AT TIME ZONE 'UTC')::date <= :to)
            GROUP BY w.id
            ORDER BY w.started_at DESC
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public WorkoutQueryService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WorkoutSummaryDto> list(LocalDate from, LocalDate to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(SELECT, params, (rs, n) -> new WorkoutSummaryDto(
                rs.getLong("id"),
                rs.getString("title"),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("ended_at")),
                (Integer) rs.getObject("duration_seconds"),
                rs.getString("split_category"),
                rs.getInt("exercise_count"),
                rs.getInt("set_count"),
                rs.getBigDecimal("working_volume")));
    }

    private static java.time.Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
