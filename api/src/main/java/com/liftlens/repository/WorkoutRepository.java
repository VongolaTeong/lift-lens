package com.liftlens.repository;

import com.liftlens.domain.Workout;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    Optional<Workout> findByHevyNaturalKey(String hevyNaturalKey);
}
