package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;

import com.liftlens.domain.ImportSource;
import com.liftlens.ingest.ImportService;
import com.liftlens.insight.InsightDetectionService;
import com.liftlens.insight.InsightDetectionService.InsightRunSummary;
import com.liftlens.insight.InsightDetector;
import com.liftlens.insight.InsightType;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Drives the detectors over a crafted history (recent enough relative to "now") that triggers every
 * insight type, then checks the resolve lifecycle, idempotency, and detector registry.
 */
class InsightDetectionIT extends AbstractPostgresIT {

    private static final String HEADER =
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,"
                    + "set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final LocalDate baseMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    @Autowired
    ImportService importService;
    @Autowired
    InsightDetectionService detectionService;
    @Autowired
    List<InsightDetector> detectors;
    @Autowired
    JdbcTemplate jdbc;

    private LocalDate weekStartAgo(int weeksAgo) {
        return baseMonday.minusWeeks(weeksAgo);
    }

    private void addSets(List<String> rows, String title, LocalDate date, String exercise,
            String weightKg, int reps, int sets) {
        String start = date.format(DAY) + ", 07:00";
        String end = date.format(DAY) + ", 08:00";
        for (int i = 0; i < sets; i++) {
            rows.add("%s,\"%s\",\"%s\",,\"%s\",,,%d,normal,%s,%d,,,"
                    .formatted(title, start, end, exercise, i, weightKg, reps));
        }
    }

    /** Imports a history that should trigger PROGRESS, REGRESSION, PLATEAU, NEGLECT, DROPOFF,
     *  IMBALANCE and PR, then returns after the import-time recompute has materialized stats. */
    private void importCraftedHistory() {
        List<String> rows = new ArrayList<>();
        for (int k = 6; k >= 1; k--) {
            LocalDate date = weekStartAgo(k);
            String squatWeight = String.valueOf(100 + (6 - k) * 2.5);   // rising -> PROGRESS
            addSets(rows, "Session", date, "Squat (Barbell)", squatWeight, 5, 3);
            addSets(rows, "Session", date, "Pull Up", "", 6 + k, 3);     // 12..7 falling -> REGRESSION
            addSets(rows, "Session", date, "Bench Press (Barbell)", "80", 5, 3); // flat -> PLATEAU
            if (k <= 4) {
                // extra recent chest volume -> CHEST >> BACK -> IMBALANCE
                addSets(rows, "Session", date, "Incline Bench Press (Dumbbell)", "30", 10, 3);
            }
        }
        // Regularly trained long ago, nothing recent -> DROPOFF
        for (int k = 20; k >= 13; k--) {
            addSets(rows, "Back", weekStartAgo(k), "Lat Pulldown (Cable)", "60", 10, 3);
        }
        byte[] csv = (HEADER + "\n" + String.join("\n", rows)).getBytes(StandardCharsets.UTF_8);
        importService.importCsv("history.csv", csv, ImportSource.CSV);
    }

    private long exerciseId(String hevyName) {
        return jdbc.queryForObject("SELECT id FROM exercise WHERE hevy_name = ?", Long.class, hevyName);
    }

    private boolean activeForExercise(InsightType type, long exerciseId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM insight WHERE type = ? AND exercise_id = ? AND status = 'ACTIVE'",
                Integer.class, type.name(), exerciseId) > 0;
    }

    private boolean activeForMuscle(InsightType type, String muscle) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM insight WHERE type = ? AND muscle = ? AND status = 'ACTIVE'",
                Integer.class, type.name(), muscle) > 0;
    }

    @Test
    void detectsEveryInsightType() {
        importCraftedHistory();

        InsightRunSummary summary = detectionService.run();
        assertThat(summary.detectorTypes()).isEqualTo(7);

        assertThat(activeForExercise(InsightType.PROGRESS, exerciseId("Squat (Barbell)"))).isTrue();
        assertThat(activeForExercise(InsightType.REGRESSION, exerciseId("Pull Up"))).isTrue();
        assertThat(activeForExercise(InsightType.PLATEAU, exerciseId("Bench Press (Barbell)"))).isTrue();
        assertThat(activeForExercise(InsightType.DROPOFF, exerciseId("Lat Pulldown (Cable)"))).isTrue();
        assertThat(activeForExercise(InsightType.PR, exerciseId("Squat (Barbell)"))).isTrue();
        assertThat(activeForMuscle(InsightType.NEGLECT, "CALVES")).isTrue();
        assertThat(activeForMuscle(InsightType.IMBALANCE, "BACK")).isTrue();
    }

    @Test
    void detectionIsIdempotent() {
        importCraftedHistory();

        detectionService.run();
        int afterFirst = jdbc.queryForObject("SELECT count(*) FROM insight", Integer.class);
        detectionService.run();
        int afterSecond = jdbc.queryForObject("SELECT count(*) FROM insight", Integer.class);

        assertThat(afterSecond).isEqualTo(afterFirst);
    }

    @Test
    void resolvesInsightsWhoseConditionNoLongerHolds() {
        importCraftedHistory();
        // An ACTIVE insight from a previous run whose window no detector will re-emit.
        long pullUpId = exerciseId("Pull Up");
        jdbc.update("INSERT INTO insight (type, exercise_id, severity, title, window_end, status) "
                + "VALUES ('REGRESSION', ?, 'WARN', 'stale', DATE '2000-01-03', 'ACTIVE')", pullUpId);

        detectionService.run();

        String status = jdbc.queryForObject(
                "SELECT status FROM insight WHERE window_end = DATE '2000-01-03'", String.class);
        assertThat(status).isEqualTo("RESOLVED");
        // The genuinely-current Pull Up regression is still active.
        assertThat(activeForExercise(InsightType.REGRESSION, pullUpId)).isTrue();
    }

    @Test
    void everyInsightTypeHasARegisteredDetector() {
        Set<InsightType> registered = detectors.stream()
                .map(InsightDetector::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(InsightType.class)));
        assertThat(registered).containsExactlyInAnyOrder(InsightType.values());
    }
}
