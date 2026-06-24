package com.liftlens.repository;

import com.liftlens.domain.Exercise;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Optional<Exercise> findByHevyName(String hevyName);

    List<Exercise> findByPrimaryMuscle(String primaryMuscle);
}
