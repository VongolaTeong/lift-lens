package com.liftlens.ingest;

import com.liftlens.domain.ImportBatch;
import java.util.List;

/**
 * Result of an import. {@code status} is {@code COMPLETED} for a fresh import or
 * {@code ALREADY_IMPORTED} when the file checksum was seen before (short-circuit).
 * {@code unknownExercises} are Hevy names that need a muscle mapping.
 */
public record ImportSummary(
        Long batchId,
        String status,
        int rowsParsed,
        int workoutsAdded,
        int workoutsMatched,
        int setsAdded,
        List<String> unknownExercises) {

    static ImportSummary alreadyImported(ImportBatch existing) {
        return new ImportSummary(existing.getId(), "ALREADY_IMPORTED", existing.getRowCount(),
                0, 0, 0, List.of());
    }
}
