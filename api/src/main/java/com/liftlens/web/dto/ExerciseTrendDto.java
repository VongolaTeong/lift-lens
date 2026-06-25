package com.liftlens.web.dto;

import java.util.List;

/**
 * An exercise's trend series for charts. {@code weighted} tells the UI which series to plot: e1RM +
 * volume for weighted lifts, the reps series for bodyweight lifts (CLAUDE.md §5, §8).
 */
public record ExerciseTrendDto(
        long exerciseId,
        String exerciseName,
        boolean weighted,
        List<TrendPointDto> points) {
}
