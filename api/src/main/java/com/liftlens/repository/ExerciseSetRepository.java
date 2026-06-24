package com.liftlens.repository;

import com.liftlens.domain.ExerciseSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {

    List<ExerciseSet> findByWorkoutId(Long workoutId);
}
