package com.liftlens.ingest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One CSV row (a single set) after header detection, unit normalization (kg + metres) and date
 * parsing. Nullable numeric fields stay null rather than being coerced to zero — bodyweight sets
 * keep {@code weightKg == null} (CLAUDE.md §5).
 */
public record ParsedSetRow(
        String workoutTitle,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String description,
        String exerciseTitle,
        String supersetId,
        String exerciseNotes,
        int setIndex,
        String setType,
        BigDecimal weightKg,
        Integer reps,
        BigDecimal distanceM,
        Integer durationSeconds,
        BigDecimal rpe) {
}
