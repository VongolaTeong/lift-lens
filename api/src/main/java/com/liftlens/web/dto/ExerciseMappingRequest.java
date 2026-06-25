package com.liftlens.web.dto;

import com.liftlens.domain.MovementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Assign a muscle mapping to an exercise (CLAUDE.md §7 — {@code PUT /api/exercises/{id}/mapping}).
 * {@code secondaryMuscles}, {@code equipment} and {@code unilateral} are optional refinements.
 */
public record ExerciseMappingRequest(
        @NotBlank String primaryMuscle,
        List<String> secondaryMuscles,
        String equipment,
        @NotNull MovementType movementType,
        Boolean unilateral) {

    public List<String> secondaryMusclesOrEmpty() {
        return secondaryMuscles == null ? List.of() : secondaryMuscles;
    }

    public boolean unilateralOrFalse() {
        return Boolean.TRUE.equals(unilateral);
    }
}
