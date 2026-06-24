package com.liftlens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Reference dimension: a Hevy exercise mapped to a canonical name and muscle grouping.
 * Unknown Hevy names are stubbed with {@link #UNKNOWN_MUSCLE} and surfaced for mapping.
 */
@Entity
@Table(name = "exercise")
public class Exercise {

    public static final String UNKNOWN_MUSCLE = "UNKNOWN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hevy_name", nullable = false, unique = true)
    private String hevyName;

    @Column(name = "canonical_name", nullable = false)
    private String canonicalName;

    @Column(name = "primary_muscle", nullable = false)
    private String primaryMuscle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "secondary_muscles", nullable = false, columnDefinition = "jsonb")
    private List<String> secondaryMuscles = new ArrayList<>();

    @Column(name = "equipment")
    private String equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType = MovementType.UNKNOWN;

    @Column(name = "is_unilateral", nullable = false)
    private boolean unilateral = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Exercise() {
        // for JPA
    }

    /** Creates a stub for an unmapped Hevy name (flagged via {@link #UNKNOWN_MUSCLE}). */
    public static Exercise stub(String hevyName) {
        Exercise e = new Exercise();
        e.hevyName = hevyName;
        e.canonicalName = hevyName;
        e.primaryMuscle = UNKNOWN_MUSCLE;
        e.movementType = MovementType.UNKNOWN;
        return e;
    }

    public boolean isUnmapped() {
        return UNKNOWN_MUSCLE.equals(primaryMuscle);
    }

    public Long getId() {
        return id;
    }

    public String getHevyName() {
        return hevyName;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getPrimaryMuscle() {
        return primaryMuscle;
    }

    public List<String> getSecondaryMuscles() {
        return secondaryMuscles;
    }

    public String getEquipment() {
        return equipment;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public boolean isUnilateral() {
        return unilateral;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
