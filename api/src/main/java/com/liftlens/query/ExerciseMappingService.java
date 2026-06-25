package com.liftlens.query;

import com.liftlens.domain.Exercise;
import com.liftlens.repository.ExerciseRepository;
import com.liftlens.web.dto.ExerciseMappingRequest;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies an admin muscle mapping to an exercise ({@code PUT /api/exercises/{id}/mapping}). A remap
 * changes future materialization; the nightly/triggered recompute job picks up the new muscle — we
 * don't recompute inline here.
 */
@Service
public class ExerciseMappingService {

    private final ExerciseRepository exercises;

    public ExerciseMappingService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Transactional
    public void applyMapping(long exerciseId, ExerciseMappingRequest request) {
        Exercise exercise = exercises.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("Exercise " + exerciseId + " not found"));
        exercise.applyMapping(
                request.primaryMuscle(),
                request.secondaryMusclesOrEmpty(),
                request.equipment(),
                request.movementType(),
                request.unilateralOrFalse());
        exercises.save(exercise);
    }
}
