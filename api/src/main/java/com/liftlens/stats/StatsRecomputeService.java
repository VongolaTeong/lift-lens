package com.liftlens.stats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializes the derived stat tables (CLAUDE.md §6 {@code recomputeStatsJob}). The analytics
 * (e1RM, volume, trend slope) are computed in Postgres with window functions deliberately — that
 * SQL is the headline backend feature.
 *
 * <p>All recompute is idempotent: a date range / exercise set is deleted then re-inserted, so
 * re-running yields identical rows. Daily and muscle-weekly are scoped by date range (expanded to
 * whole ISO weeks); per-exercise weekly is recomputed across the exercise's full history so the
 * trailing-window slopes stay correct. e1RM/best-rep slopes use {@code regr_slope} over a trailing
 * 8-week window.
 */
@Service
public class StatsRecomputeService {

    private static final Logger log = LoggerFactory.getLogger(StatsRecomputeService.class);

    // Wide-but-valid sentinel range for a full refresh.
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2999, 12, 31);

    private static final String DELETE_DAILY =
            "DELETE FROM exercise_daily_stat WHERE stat_date BETWEEN :from AND :to";

    private static final String INSERT_DAILY = """
            INSERT INTO exercise_daily_stat
                (exercise_id, stat_date, top_working_e1rm, working_volume, working_sets, max_weight, total_reps)
            SELECT
                es.exercise_id,
                (w.started_at AT TIME ZONE 'UTC')::date,
                MAX(CASE WHEN es.weight_kg IS NOT NULL AND es.reps IS NOT NULL AND es.reps <= 12
                         THEN es.weight_kg * (1 + es.reps / 30.0) END),
                COALESCE(SUM(CASE WHEN es.weight_kg IS NOT NULL AND es.reps IS NOT NULL
                         THEN es.weight_kg * es.reps ELSE 0 END), 0),
                COUNT(*),
                MAX(es.weight_kg),
                COALESCE(SUM(es.reps), 0)
            FROM exercise_set es
            JOIN workout w ON w.id = es.workout_id
            WHERE es.is_working
              AND (w.started_at AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
            GROUP BY es.exercise_id, (w.started_at AT TIME ZONE 'UTC')::date
            """;

    private static final String DELETE_MUSCLE_WEEKS = """
            DELETE FROM muscle_weekly_volume WHERE (iso_year, iso_week) IN (
                SELECT DISTINCT EXTRACT(ISOYEAR FROM (started_at AT TIME ZONE 'UTC'))::int,
                                EXTRACT(WEEK    FROM (started_at AT TIME ZONE 'UTC'))::int
                FROM workout
                WHERE (started_at AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
            )
            """;

    private static final String INSERT_MUSCLE = """
            INSERT INTO muscle_weekly_volume
                (muscle, iso_year, iso_week, working_volume, set_count, session_count)
            SELECT
                e.primary_muscle,
                EXTRACT(ISOYEAR FROM (w.started_at AT TIME ZONE 'UTC'))::int,
                EXTRACT(WEEK    FROM (w.started_at AT TIME ZONE 'UTC'))::int,
                COALESCE(SUM(CASE WHEN es.weight_kg IS NOT NULL AND es.reps IS NOT NULL
                         THEN es.weight_kg * es.reps ELSE 0 END), 0),
                COUNT(*),
                COUNT(DISTINCT es.workout_id)
            FROM exercise_set es
            JOIN workout w ON w.id = es.workout_id
            JOIN exercise e ON e.id = es.exercise_id
            WHERE es.is_working
              AND (w.started_at AT TIME ZONE 'UTC')::date BETWEEN :from AND :to
            GROUP BY e.primary_muscle,
                     EXTRACT(ISOYEAR FROM (w.started_at AT TIME ZONE 'UTC')),
                     EXTRACT(WEEK    FROM (w.started_at AT TIME ZONE 'UTC'))
            """;

    private static final String DELETE_WEEKLY =
            "DELETE FROM exercise_weekly_stat WHERE exercise_id IN (:ids)";

    private static final String INSERT_WEEKLY = """
            INSERT INTO exercise_weekly_stat
                (exercise_id, iso_year, iso_week, best_e1rm, volume, sets, sessions,
                 best_reps, total_reps, e1rm_slope, reps_slope)
            WITH s AS (
                SELECT es.exercise_id, es.workout_id, es.weight_kg, es.reps,
                       (w.started_at AT TIME ZONE 'UTC') AS lt
                FROM exercise_set es
                JOIN workout w ON w.id = es.workout_id
                WHERE es.is_working AND es.exercise_id IN (:ids)
            ),
            weekly AS (
                SELECT exercise_id,
                    EXTRACT(ISOYEAR FROM lt)::int AS iso_year,
                    EXTRACT(WEEK    FROM lt)::int AS iso_week,
                    floor(EXTRACT(EPOCH FROM date_trunc('week', lt)) / 604800)::int AS wk_ordinal,
                    MAX(CASE WHEN weight_kg IS NOT NULL AND reps IS NOT NULL AND reps <= 12
                             THEN weight_kg * (1 + reps / 30.0) END) AS best_e1rm,
                    COALESCE(SUM(CASE WHEN weight_kg IS NOT NULL AND reps IS NOT NULL
                             THEN weight_kg * reps ELSE 0 END), 0) AS volume,
                    COUNT(*) AS sets,
                    COUNT(DISTINCT workout_id) AS sessions,
                    MAX(reps) AS best_reps,
                    COALESCE(SUM(reps), 0) AS total_reps
                FROM s
                GROUP BY exercise_id,
                         EXTRACT(ISOYEAR FROM lt), EXTRACT(WEEK FROM lt),
                         floor(EXTRACT(EPOCH FROM date_trunc('week', lt)) / 604800)
            )
            SELECT exercise_id, iso_year, iso_week, best_e1rm, volume, sets, sessions,
                   best_reps, total_reps,
                   regr_slope(best_e1rm, wk_ordinal) OVER win,
                   regr_slope(best_reps::numeric, wk_ordinal) OVER win
            FROM weekly
            WINDOW win AS (PARTITION BY exercise_id ORDER BY wk_ordinal
                           RANGE BETWEEN 7 PRECEDING AND CURRENT ROW)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final PrScanService prScanService;

    public StatsRecomputeService(NamedParameterJdbcTemplate jdbc, PrScanService prScanService) {
        this.jdbc = jdbc;
        this.prScanService = prScanService;
    }

    /** Full refresh of every materialized table for the given exercises (all of them, normally). */
    @Transactional
    public void recomputeAll(Collection<Long> allExerciseIds) {
        recompute(MIN_DATE, MAX_DATE, allExerciseIds);
    }

    /**
     * Incremental, idempotent recompute for an affected date range and the affected exercises.
     * The range is expanded to whole ISO weeks so weekly aggregates are never partial.
     */
    @Transactional
    public void recompute(LocalDate from, LocalDate to, Collection<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) {
            return;
        }
        LocalDate weekFrom = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekTo = to.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Map<String, Object> range = new HashMap<>();
        range.put("from", weekFrom);
        range.put("to", weekTo);
        Map<String, Object> ids = Map.of("ids", exerciseIds);

        jdbc.update(DELETE_DAILY, range);
        jdbc.update(INSERT_DAILY, range);

        jdbc.update(DELETE_MUSCLE_WEEKS, range);
        jdbc.update(INSERT_MUSCLE, range);

        jdbc.update(DELETE_WEEKLY, ids);
        jdbc.update(INSERT_WEEKLY, ids);

        prScanService.scan(exerciseIds);

        log.info("Recomputed stats for {} exercises over {}..{}", exerciseIds.size(), weekFrom, weekTo);
    }
}
