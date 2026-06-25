package com.liftlens.web.dto;

import java.math.BigDecimal;

/**
 * This-week vs last-week working volume for a muscle, for the dashboard snapshot (CLAUDE.md §8).
 */
public record MuscleVolumeComparisonDto(
        String muscle,
        BigDecimal thisWeekVolume,
        BigDecimal lastWeekVolume,
        int thisWeekSets,
        int lastWeekSets) {
}
