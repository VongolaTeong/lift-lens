package com.liftlens.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * An exercise with its mapping status and a little usage context (working set count, last trained).
 * {@code mapped} is false for stubs awaiting a muscle mapping (CLAUDE.md §7 — list with mapping status).
 */
public record ExerciseSummaryDto(
        long id,
        String hevyName,
        String canonicalName,
        String primaryMuscle,
        List<String> secondaryMuscles,
        String equipment,
        String movementType,
        boolean unilateral,
        boolean mapped,
        int workingSets,
        LocalDate lastTrained) {
}
