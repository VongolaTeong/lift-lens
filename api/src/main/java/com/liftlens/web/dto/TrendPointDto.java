package com.liftlens.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One ISO week of an exercise's materialized trend. Weighted lifts read {@code bestE1rm}/{@code volume}
 * and {@code e1rmSlope}; bodyweight lifts read {@code bestReps}/{@code totalReps} and {@code repsSlope}
 * (CLAUDE.md §5). All metric fields are nullable so a single shape serves both series.
 */
public record TrendPointDto(
        LocalDate weekStart,
        int isoYear,
        int isoWeek,
        BigDecimal bestE1rm,
        BigDecimal volume,
        Integer bestReps,
        Integer totalReps,
        int sets,
        int sessions,
        BigDecimal e1rmSlope,
        BigDecimal repsSlope) {
}
