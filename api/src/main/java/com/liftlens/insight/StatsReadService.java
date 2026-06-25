package com.liftlens.insight;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read side over the materialized stat tables (CLAUDE.md §6), shaped for detectors. Detectors stay
 * thin and DB-free; all the SQL lives here.
 */
@Service
public class StatsReadService {

    private final JdbcTemplate jdbc;

    public StatsReadService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Per-exercise trend over its most recent (≤8) recorded weeks, using the materialized slopes. */
    public List<TrendSnapshot> trendSnapshots() {
        Map<Long, List<WeekRow>> byExercise = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        jdbc.query("""
                SELECT ews.exercise_id,
                       e.canonical_name AS name,
                       to_date(ews.iso_year || '-' || lpad(ews.iso_week::text, 2, '0'), 'IYYY-IW') AS week_start,
                       ews.best_e1rm, ews.best_reps, ews.e1rm_slope, ews.reps_slope
                FROM exercise_weekly_stat ews
                JOIN exercise e ON e.id = ews.exercise_id
                ORDER BY ews.exercise_id, ews.iso_year, ews.iso_week
                """, rs -> {
            long id = rs.getLong("exercise_id");
            names.putIfAbsent(id, rs.getString("name"));
            byExercise.computeIfAbsent(id, k -> new ArrayList<>()).add(new WeekRow(
                    rs.getObject("week_start", LocalDate.class),
                    (BigDecimal) rs.getObject("best_e1rm"),
                    (Integer) rs.getObject("best_reps"),
                    (BigDecimal) rs.getObject("e1rm_slope"),
                    (BigDecimal) rs.getObject("reps_slope")));
        });

        List<TrendSnapshot> out = new ArrayList<>();
        for (var entry : byExercise.entrySet()) {
            List<WeekRow> all = entry.getValue();
            List<WeekRow> window = all.subList(Math.max(0, all.size() - 8), all.size());
            boolean weighted = window.stream().anyMatch(r -> r.bestE1rm() != null);
            WeekRow latest = window.get(window.size() - 1);
            BigDecimal slope = weighted ? latest.e1rmSlope() : latest.repsSlope();
            int points = (int) window.stream()
                    .filter(r -> (weighted ? r.bestE1rm() : r.bestReps()) != null)
                    .count();
            out.add(new TrendSnapshot(entry.getKey(), names.get(entry.getKey()), weighted,
                    window.get(0).weekStart(), latest.weekStart(),
                    slope == null ? Double.NaN : slope.doubleValue(), points));
        }
        return out;
    }

    /** Weekly per-muscle volume/sets, ordered. Used by neglect + imbalance. */
    public List<MuscleWeekPoint> muscleWeeklyPoints() {
        return jdbc.query("""
                SELECT muscle,
                       to_date(iso_year || '-' || lpad(iso_week::text, 2, '0'), 'IYYY-IW') AS week_start,
                       working_volume, set_count, session_count
                FROM muscle_weekly_volume
                ORDER BY muscle, week_start
                """, (rs, n) -> new MuscleWeekPoint(
                rs.getString("muscle"),
                rs.getObject("week_start", LocalDate.class),
                rs.getBigDecimal("working_volume"),
                rs.getInt("set_count"),
                rs.getInt("session_count")));
    }

    /** Known (mapped) primary muscles the athlete actually trains. */
    public List<String> trainedMuscles() {
        return jdbc.queryForList(
                "SELECT DISTINCT primary_muscle FROM exercise WHERE primary_muscle <> 'UNKNOWN' "
                        + "ORDER BY primary_muscle", String.class);
    }

    /** Per-exercise lifetime session count + last session date (working sets only). */
    public List<ExerciseHistory> exerciseHistories() {
        return jdbc.query("""
                SELECT e.id, e.canonical_name AS name, e.primary_muscle,
                       count(DISTINCT w.id) AS sessions,
                       max((w.started_at AT TIME ZONE 'UTC')::date) AS last_session
                FROM exercise e
                JOIN exercise_set es ON es.exercise_id = e.id AND es.is_working
                JOIN workout w ON w.id = es.workout_id
                GROUP BY e.id, e.canonical_name, e.primary_muscle
                """, (rs, n) -> new ExerciseHistory(
                rs.getLong("id"), rs.getString("name"), rs.getString("primary_muscle"),
                rs.getInt("sessions"), rs.getObject("last_session", LocalDate.class)));
    }

    /** PR sets on or after {@code since}, newest first per exercise. */
    public List<PrRef> prsSince(LocalDate since) {
        return jdbc.query("""
                SELECT es.exercise_id, e.canonical_name AS name,
                       (w.started_at AT TIME ZONE 'UTC')::date AS d,
                       es.load_basis, es.weight_kg, es.reps,
                       CASE WHEN es.weight_kg IS NOT NULL AND es.reps <= 12
                            THEN es.weight_kg * (1 + es.reps / 30.0) END AS e1rm,
                       CASE WHEN es.weight_kg IS NOT NULL THEN es.weight_kg * es.reps END AS vol
                FROM exercise_set es
                JOIN workout w ON w.id = es.workout_id
                JOIN exercise e ON e.id = es.exercise_id
                WHERE es.is_pr AND (w.started_at AT TIME ZONE 'UTC')::date >= ?
                ORDER BY es.exercise_id, d DESC
                """, (rs, n) -> new PrRef(
                rs.getLong("exercise_id"), rs.getString("name"),
                rs.getObject("d", LocalDate.class), rs.getString("load_basis"),
                (BigDecimal) rs.getObject("weight_kg"), (Integer) rs.getObject("reps"),
                (BigDecimal) rs.getObject("e1rm"), (BigDecimal) rs.getObject("vol")), since);
    }

    private record WeekRow(LocalDate weekStart, BigDecimal bestE1rm, Integer bestReps,
            BigDecimal e1rmSlope, BigDecimal repsSlope) {
    }

    public record TrendSnapshot(long exerciseId, String exerciseName, boolean weighted,
            LocalDate windowStart, LocalDate windowEnd, double slope, int points) {
    }

    public record MuscleWeekPoint(String muscle, LocalDate weekStart, BigDecimal volume,
            int setCount, int sessionCount) {
    }

    public record ExerciseHistory(long exerciseId, String exerciseName, String primaryMuscle,
            int sessions, LocalDate lastSession) {
    }

    public record PrRef(long exerciseId, String exerciseName, LocalDate date, String loadBasis,
            BigDecimal weightKg, Integer reps, BigDecimal e1rm, BigDecimal volume) {
    }
}
