package com.liftlens.domain;

/**
 * Which progression series a set feeds: weighted movements trend on e1RM/volume, bodyweight
 * movements (empty {@code weight_kg}) trend on reps (CLAUDE.md §5). Derived per set at ingest.
 */
public enum LoadBasis {
    WEIGHTED,
    BODYWEIGHT
}
