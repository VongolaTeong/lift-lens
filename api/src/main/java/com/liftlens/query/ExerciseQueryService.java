package com.liftlens.query;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftlens.domain.Exercise;
import com.liftlens.web.dto.ExerciseSummaryDto;
import com.liftlens.web.dto.ExerciseTrendDto;
import com.liftlens.web.dto.TrendPointDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * Read side for the exercise endpoints (CLAUDE.md §7): list-with-mapping-status, the unmapped feed,
 * and the per-exercise trend series. SQL-based to stay consistent with the analytics read layer.
 */
@Service
public class ExerciseQueryService {

    private static final String SUMMARY_SELECT = """
            SELECT e.id, e.hevy_name, e.canonical_name, e.primary_muscle, e.secondary_muscles,
                   e.equipment, e.movement_type, e.is_unilateral,
                   COUNT(es.id) FILTER (WHERE es.is_working) AS working_sets,
                   MAX((w.started_at AT TIME ZONE 'UTC')::date) FILTER (WHERE es.is_working) AS last_trained
            FROM exercise e
            LEFT JOIN exercise_set es ON es.exercise_id = e.id
            LEFT JOIN workout w ON w.id = es.workout_id
            %s
            GROUP BY e.id
            ORDER BY e.canonical_name
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ExerciseQueryService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /** All exercises, alphabetised, with mapping status and basic usage context. */
    public List<ExerciseSummaryDto> list() {
        return jdbc.query(SUMMARY_SELECT.formatted(""), summaryMapper());
    }

    /** Exercises still awaiting a muscle mapping (stubs from unknown Hevy names). */
    public List<ExerciseSummaryDto> unmapped() {
        return jdbc.query(SUMMARY_SELECT.formatted("WHERE e.primary_muscle = '" + Exercise.UNKNOWN_MUSCLE + "'"),
                summaryMapper());
    }

    /** Single exercise summary; 404 if it does not exist. */
    public ExerciseSummaryDto getById(long exerciseId) {
        List<ExerciseSummaryDto> rows = jdbc.query(SUMMARY_SELECT.formatted("WHERE e.id = ?"),
                summaryMapper(), exerciseId);
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Exercise " + exerciseId + " not found");
        }
        return rows.get(0);
    }

    /** Most recent {@code weeks} of materialized weekly stats for an exercise; 404 if it does not exist. */
    public ExerciseTrendDto trend(long exerciseId, int weeks) {
        String name;
        try {
            name = jdbc.queryForObject("SELECT canonical_name FROM exercise WHERE id = ?",
                    String.class, exerciseId);
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchElementException("Exercise " + exerciseId + " not found");
        }

        List<TrendPointDto> all = jdbc.query("""
                SELECT iso_year, iso_week,
                       to_date(iso_year || '-' || lpad(iso_week::text, 2, '0'), 'IYYY-IW') AS week_start,
                       best_e1rm, volume, best_reps, total_reps, sets, sessions, e1rm_slope, reps_slope
                FROM exercise_weekly_stat
                WHERE exercise_id = ?
                ORDER BY iso_year, iso_week
                """, (rs, n) -> new TrendPointDto(
                rs.getObject("week_start", LocalDate.class),
                rs.getInt("iso_year"),
                rs.getInt("iso_week"),
                rs.getBigDecimal("best_e1rm"),
                rs.getBigDecimal("volume"),
                (Integer) rs.getObject("best_reps"),
                (Integer) rs.getObject("total_reps"),
                rs.getInt("sets"),
                rs.getInt("sessions"),
                rs.getBigDecimal("e1rm_slope"),
                rs.getBigDecimal("reps_slope")), exerciseId);

        int limit = Math.max(1, weeks);
        List<TrendPointDto> window = new ArrayList<>(
                all.subList(Math.max(0, all.size() - limit), all.size()));
        boolean weighted = window.stream().anyMatch(p -> p.bestE1rm() != null);
        return new ExerciseTrendDto(exerciseId, name, weighted, window);
    }

    private RowMapper<ExerciseSummaryDto> summaryMapper() {
        return (rs, n) -> {
            String primaryMuscle = rs.getString("primary_muscle");
            return new ExerciseSummaryDto(
                    rs.getLong("id"),
                    rs.getString("hevy_name"),
                    rs.getString("canonical_name"),
                    primaryMuscle,
                    parseStringList(rs.getString("secondary_muscles")),
                    rs.getString("equipment"),
                    rs.getString("movement_type"),
                    rs.getBoolean("is_unilateral"),
                    !Exercise.UNKNOWN_MUSCLE.equals(primaryMuscle),
                    rs.getInt("working_sets"),
                    rs.getObject("last_trained", LocalDate.class));
        };
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }
}
