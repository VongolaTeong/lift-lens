package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;

import com.liftlens.analytics.EstimatedOneRepMax;
import com.liftlens.domain.Exercise;
import com.liftlens.domain.ImportSource;
import com.liftlens.ingest.ImportService;
import com.liftlens.repository.ExerciseRepository;
import com.liftlens.stats.StatsRecomputeService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies the materialized stats over deterministic data: three weekly sessions where the squat
 * gets heavier and the pull-up gets more reps. Confirms weighted e1RM trends, the bodyweight
 * reps series trends (no e1RM), PR flags, and that recompute is idempotent.
 */
class StatsMaterializationIT extends AbstractPostgresIT {

    private static final String HEADER =
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,"
                    + "set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe";

    @Autowired
    ImportService importService;
    @Autowired
    StatsRecomputeService statsRecomputeService;
    @Autowired
    ExerciseRepository exerciseRepository;
    @Autowired
    JdbcTemplate jdbc;

    private static String row(String start, String exercise, int idx, String weightKg, int reps) {
        return "Day,\"%s\",\"%s\",,\"%s\",,,%d,normal,%s,%d,,,"
                .formatted(start, start, exercise, idx, weightKg, reps);
    }

    /** Three consecutive Mondays — each its own ISO week — with rising squat load and pull-up reps. */
    private static byte[] progressionCsv() {
        return (HEADER + "\n" + String.join("\n",
                row("5 Jan 2026, 07:00", "Squat (Barbell)", 0, "100", 5),
                row("5 Jan 2026, 07:00", "Pull Up", 0, "", 8),
                row("12 Jan 2026, 07:00", "Squat (Barbell)", 0, "105", 5),
                row("12 Jan 2026, 07:00", "Pull Up", 0, "", 9),
                row("19 Jan 2026, 07:00", "Squat (Barbell)", 0, "110", 5),
                row("19 Jan 2026, 07:00", "Pull Up", 0, "", 10)))
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void weightedExerciseMaterializesDailyE1rmAndPositiveSlope() {
        importService.importCsv("prog.csv", progressionCsv(), ImportSource.CSV);

        // Daily e1RM matches the analytics formula (SQL mirrors EstimatedOneRepMax.epley).
        BigDecimal e1rm = jdbc.queryForObject(
                "SELECT ds.top_working_e1rm FROM exercise_daily_stat ds "
                        + "JOIN exercise e ON e.id = ds.exercise_id "
                        + "WHERE e.hevy_name = 'Squat (Barbell)' AND ds.stat_date = DATE '2026-01-19'",
                BigDecimal.class);
        assertThat(e1rm).isEqualByComparingTo(
                BigDecimal.valueOf(EstimatedOneRepMax.epley(110, 5)).setScale(2, java.math.RoundingMode.HALF_UP));

        BigDecimal volume = jdbc.queryForObject(
                "SELECT ds.working_volume FROM exercise_daily_stat ds "
                        + "JOIN exercise e ON e.id = ds.exercise_id "
                        + "WHERE e.hevy_name = 'Squat (Barbell)' AND ds.stat_date = DATE '2026-01-19'",
                BigDecimal.class);
        assertThat(volume).isEqualByComparingTo("550"); // 110 x 5

        // Weekly e1RM trend is positive (regr_slope over the rising weeks).
        BigDecimal slope = jdbc.queryForObject(
                "SELECT max(ews.e1rm_slope) FROM exercise_weekly_stat ews "
                        + "JOIN exercise e ON e.id = ews.exercise_id WHERE e.hevy_name = 'Squat (Barbell)'",
                BigDecimal.class);
        assertThat(slope).isNotNull();
        assertThat(slope.doubleValue()).isPositive();
    }

    @Test
    void bodyweightExerciseTrendsOnRepsNotE1rm() {
        importService.importCsv("prog.csv", progressionCsv(), ImportSource.CSV);

        // Pull Up has no e1RM at all (weight is null) ...
        Integer weeksWithE1rm = jdbc.queryForObject(
                "SELECT count(*) FROM exercise_weekly_stat ews JOIN exercise e ON e.id = ews.exercise_id "
                        + "WHERE e.hevy_name = 'Pull Up' AND ews.best_e1rm IS NOT NULL", Integer.class);
        assertThat(weeksWithE1rm).isZero();

        // ... but it does trend on reps.
        BigDecimal repsSlope = jdbc.queryForObject(
                "SELECT max(ews.reps_slope) FROM exercise_weekly_stat ews "
                        + "JOIN exercise e ON e.id = ews.exercise_id WHERE e.hevy_name = 'Pull Up'",
                BigDecimal.class);
        assertThat(repsSlope).isNotNull();
        assertThat(repsSlope.doubleValue()).isPositive();

        Integer bestReps = jdbc.queryForObject(
                "SELECT max(ews.best_reps) FROM exercise_weekly_stat ews "
                        + "JOIN exercise e ON e.id = ews.exercise_id WHERE e.hevy_name = 'Pull Up'",
                Integer.class);
        assertThat(bestReps).isEqualTo(10);
    }

    @Test
    void flagsPersonalRecords() {
        importService.importCsv("prog.csv", progressionCsv(), ImportSource.CSV);

        Boolean squatPr = jdbc.queryForObject(
                "SELECT es.is_pr FROM exercise_set es JOIN exercise e ON e.id = es.exercise_id "
                        + "WHERE e.hevy_name = 'Squat (Barbell)' AND es.weight_kg = 110", Boolean.class);
        assertThat(squatPr).isTrue();

        Boolean pullUpPr = jdbc.queryForObject(
                "SELECT bool_or(es.is_pr) FROM exercise_set es JOIN exercise e ON e.id = es.exercise_id "
                        + "WHERE e.hevy_name = 'Pull Up' AND es.reps = 10", Boolean.class);
        assertThat(pullUpPr).isTrue();
    }

    @Test
    void recomputeIsIdempotent() {
        importService.importCsv("prog.csv", progressionCsv(), ImportSource.CSV);

        String weeklySnapshot = "SELECT exercise_id, iso_year, iso_week, best_e1rm, volume, sets, "
                + "best_reps, total_reps, e1rm_slope, reps_slope FROM exercise_weekly_stat "
                + "ORDER BY exercise_id, iso_year, iso_week";
        List<Map<String, Object>> before = jdbc.queryForList(weeklySnapshot);

        List<Long> allIds = exerciseRepository.findAll().stream().map(Exercise::getId).toList();
        statsRecomputeService.recomputeAll(allIds);

        List<Map<String, Object>> after = jdbc.queryForList(weeklySnapshot);
        assertThat(after).isEqualTo(before);
    }
}
