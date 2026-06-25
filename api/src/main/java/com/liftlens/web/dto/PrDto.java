package com.liftlens.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A recent personal record (CLAUDE.md §7 dashboard). {@code loadBasis} distinguishes weighted PRs
 * (weight/e1RM/volume) from bodyweight rep PRs; the irrelevant metrics are null.
 */
public record PrDto(
        long exerciseId,
        String exerciseName,
        LocalDate date,
        String loadBasis,
        BigDecimal weightKg,
        Integer reps,
        BigDecimal e1rm,
        BigDecimal volume) {
}
