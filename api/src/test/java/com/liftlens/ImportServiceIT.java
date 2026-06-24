package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;

import com.liftlens.domain.Exercise;
import com.liftlens.domain.ImportSource;
import com.liftlens.ingest.ImportService;
import com.liftlens.ingest.ImportSummary;
import com.liftlens.repository.ExerciseRepository;
import com.liftlens.repository.ExerciseSetRepository;
import com.liftlens.repository.WorkoutRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ImportServiceIT extends AbstractPostgresIT {

    private static final String HEADER =
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,"
                    + "set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe";

    @Autowired
    ImportService importService;
    @Autowired
    WorkoutRepository workoutRepository;
    @Autowired
    ExerciseSetRepository setRepository;
    @Autowired
    ExerciseRepository exerciseRepository;
    @Autowired
    JdbcTemplate jdbc;

    private static String row(String title, String start, String end, String exercise, int idx,
            String weightKg, int reps) {
        return "%s,\"%s\",\"%s\",,\"%s\",,,%d,normal,%s,%d,,,"
                .formatted(title, start, end, exercise, idx, weightKg, reps);
    }

    private static byte[] csv(String... rows) {
        return (HEADER + "\n" + String.join("\n", rows)).getBytes(StandardCharsets.UTF_8);
    }

    // Two workouts: a Lower day (weighted squats + a bodyweight pull up) and an Arms day with an
    // exercise that isn't in the seed (-> unmapped stub).
    private static byte[] twoWorkouts() {
        return csv(
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Squat (Barbell)", 0, "100", 5),
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Squat (Barbell)", 1, "100", 5),
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Pull Up", 0, "", 8),
                row("Arms", "2 Feb 2026, 07:00", "2 Feb 2026, 08:00", "Zercher Carry", 0, "50", 10));
    }

    @Test
    void importsWorkoutsAndSetsAndFlagsBodyweightAndUnknown() {
        ImportSummary summary = importService.importCsv("two.csv", twoWorkouts(), ImportSource.CSV);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.rowsParsed()).isEqualTo(4);
        assertThat(summary.workoutsAdded()).isEqualTo(2);
        assertThat(summary.setsAdded()).isEqualTo(4);
        assertThat(summary.unknownExercises()).containsExactly("Zercher Carry");

        assertThat(workoutRepository.count()).isEqualTo(2);
        assertThat(setRepository.count()).isEqualTo(4);

        // Bodyweight Pull Up: weight null, load_basis BODYWEIGHT.
        String loadBasis = jdbc.queryForObject(
                "SELECT es.load_basis FROM exercise_set es JOIN exercise e ON e.id = es.exercise_id "
                        + "WHERE e.hevy_name = 'Pull Up'", String.class);
        assertThat(loadBasis).isEqualTo("BODYWEIGHT");
        Object weight = jdbc.queryForObject(
                "SELECT es.weight_kg FROM exercise_set es JOIN exercise e ON e.id = es.exercise_id "
                        + "WHERE e.hevy_name = 'Pull Up'", Object.class);
        assertThat(weight).isNull();

        // Weighted Squat: load_basis WEIGHTED.
        String squatBasis = jdbc.queryForObject(
                "SELECT DISTINCT es.load_basis FROM exercise_set es JOIN exercise e ON e.id = es.exercise_id "
                        + "WHERE e.hevy_name = 'Squat (Barbell)'", String.class);
        assertThat(squatBasis).isEqualTo("WEIGHTED");

        // Unknown exercise was stubbed and flagged.
        Optional<Exercise> zercher = exerciseRepository.findByHevyName("Zercher Carry");
        assertThat(zercher).isPresent();
        assertThat(zercher.get().isUnmapped()).isTrue();
    }

    @Test
    void reimportingSameFileIsANoOp() {
        byte[] file = twoWorkouts();
        importService.importCsv("two.csv", file, ImportSource.CSV);

        ImportSummary second = importService.importCsv("two.csv", file, ImportSource.CSV);

        assertThat(second.status()).isEqualTo("ALREADY_IMPORTED");
        assertThat(second.workoutsAdded()).isZero();
        assertThat(second.setsAdded()).isZero();
        assertThat(workoutRepository.count()).isEqualTo(2);
        assertThat(setRepository.count()).isEqualTo(4);
    }

    @Test
    void overlappingReimportDoesNotDoubleCount() {
        byte[] fileA = csv(
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Squat (Barbell)", 0, "100", 5),
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Squat (Barbell)", 1, "100", 5),
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Pull Up", 0, "", 8));
        // fileB re-includes all of fileA's rows plus a new workout (different checksum).
        byte[] fileB = csv(
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Squat (Barbell)", 0, "100", 5),
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Squat (Barbell)", 1, "100", 5),
                row("Lower", "1 Feb 2026, 07:00", "1 Feb 2026, 08:00", "Pull Up", 0, "", 8),
                row("Arms", "2 Feb 2026, 07:00", "2 Feb 2026, 08:00", "Bicep Curl (Dumbbell)", 0, "12", 12));

        ImportSummary a = importService.importCsv("a.csv", fileA, ImportSource.CSV);
        assertThat(a.workoutsAdded()).isEqualTo(1);
        assertThat(a.setsAdded()).isEqualTo(3);

        ImportSummary b = importService.importCsv("b.csv", fileB, ImportSource.CSV);
        assertThat(b.workoutsAdded()).isEqualTo(1);   // only the Arms workout is new
        assertThat(b.workoutsMatched()).isEqualTo(1); // Lower already existed
        assertThat(b.setsAdded()).isEqualTo(1);       // only the new Bicep Curl set

        assertThat(workoutRepository.count()).isEqualTo(2);
        assertThat(setRepository.count()).isEqualTo(4);
    }
}
