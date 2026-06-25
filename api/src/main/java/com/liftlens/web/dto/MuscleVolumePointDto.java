package com.liftlens.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One ISO week of working volume for a muscle (CLAUDE.md §7 — {@code GET /api/muscles/volume}),
 * driving the balance view. The UI groups these by muscle into per-muscle weekly series.
 */
public record MuscleVolumePointDto(
        String muscle,
        LocalDate weekStart,
        int isoYear,
        int isoWeek,
        BigDecimal workingVolume,
        int setCount,
        int sessionCount) {
}
