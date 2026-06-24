package com.liftlens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One performed set. {@code weightKg} is null for bodyweight movements; {@code loadBasis} records
 * which progression series this set feeds. Unique on (workout, exercise, set_index) so re-imports
 * of overlapping exports never double-count.
 */
@Entity
@Table(name = "exercise_set",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_exercise_set", columnNames = {"workout_id", "exercise_id", "set_index"}))
public class ExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_index", nullable = false)
    private int setIndex;

    @Column(name = "set_type", nullable = false)
    private String setType = "normal";

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "rpe")
    private BigDecimal rpe;

    @Column(name = "distance_m")
    private BigDecimal distanceM;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_working", nullable = false)
    private boolean working = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "load_basis", nullable = false)
    private LoadBasis loadBasis = LoadBasis.WEIGHTED;

    @Column(name = "is_pr", nullable = false)
    private boolean pr = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ExerciseSet() {
        // built field-by-field at ingest; also satisfies JPA
    }

    public Long getId() {
        return id;
    }

    public Workout getWorkout() {
        return workout;
    }

    public void setWorkout(Workout workout) {
        this.workout = workout;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public int getSetIndex() {
        return setIndex;
    }

    public void setSetIndex(int setIndex) {
        this.setIndex = setIndex;
    }

    public String getSetType() {
        return setType;
    }

    public void setSetType(String setType) {
        this.setType = setType;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public BigDecimal getRpe() {
        return rpe;
    }

    public void setRpe(BigDecimal rpe) {
        this.rpe = rpe;
    }

    public BigDecimal getDistanceM() {
        return distanceM;
    }

    public void setDistanceM(BigDecimal distanceM) {
        this.distanceM = distanceM;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
    }

    public LoadBasis getLoadBasis() {
        return loadBasis;
    }

    public void setLoadBasis(LoadBasis loadBasis) {
        this.loadBasis = loadBasis;
    }

    public boolean isPr() {
        return pr;
    }

    public void setPr(boolean pr) {
        this.pr = pr;
    }
}
