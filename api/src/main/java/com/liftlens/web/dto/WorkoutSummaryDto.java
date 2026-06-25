package com.liftlens.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A session in the workout list (CLAUDE.md §7 — {@code GET /api/workouts}). Set/exercise counts and
 * working volume are aggregated from the session's working sets.
 */
public record WorkoutSummaryDto(
        long id,
        String title,
        Instant startedAt,
        Instant endedAt,
        Integer durationSeconds,
        String splitCategory,
        int exerciseCount,
        int setCount,
        BigDecimal workingVolume) {
}
