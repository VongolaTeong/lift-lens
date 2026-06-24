package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.liftlens.domain.ImportSource;
import com.liftlens.ingest.ImportService;
import com.liftlens.ingest.ImportSummary;
import com.liftlens.repository.ExerciseRepository;
import com.liftlens.repository.ExerciseSetRepository;
import com.liftlens.repository.WorkoutRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Imports the real Hevy export and asserts the locked counts (CLAUDE.md §2): 324 workouts,
 * 6,345 sets, 40 exercises — all of which are seeded, so nothing comes back unmapped.
 * Skipped when {@code workout_data.csv} isn't present (it's gitignored), so CI stays green.
 */
class RealExportIT extends AbstractPostgresIT {

    @Autowired
    ImportService importService;
    @Autowired
    WorkoutRepository workoutRepository;
    @Autowired
    ExerciseSetRepository setRepository;
    @Autowired
    ExerciseRepository exerciseRepository;

    private static byte[] realExportOrNull() throws IOException {
        for (Path candidate : List.of(Path.of("workout_data.csv"), Path.of("..", "workout_data.csv"))) {
            if (Files.exists(candidate)) {
                return Files.readAllBytes(candidate);
            }
        }
        return null;
    }

    @Test
    void importsRealExportWithLockedCountsAndNoUnknowns() throws IOException {
        byte[] bytes = realExportOrNull();
        assumeTrue(bytes != null, "workout_data.csv not present — skipping real-export IT");

        ImportSummary summary = importService.importCsv("workout_data.csv", bytes, ImportSource.CSV);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.workoutsAdded()).isEqualTo(324);
        assertThat(summary.setsAdded()).isEqualTo(6345);
        assertThat(summary.unknownExercises()).isEmpty();

        assertThat(workoutRepository.count()).isEqualTo(324);
        assertThat(setRepository.count()).isEqualTo(6345);
        assertThat(exerciseRepository.count()).isEqualTo(40); // all seeded; no stubs created

        // Idempotent re-import of the same file.
        ImportSummary again = importService.importCsv("workout_data.csv", bytes, ImportSource.CSV);
        assertThat(again.status()).isEqualTo("ALREADY_IMPORTED");
        assertThat(workoutRepository.count()).isEqualTo(324);
        assertThat(setRepository.count()).isEqualTo(6345);
    }
}
