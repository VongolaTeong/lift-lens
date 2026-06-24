package com.liftlens.analytics;

/**
 * Estimated 1RM formulas (CLAUDE.md §5). Pure functions — the canonical source of truth for e1RM;
 * the materialization SQL mirrors {@link #epley(double, int)} and must stay in sync.
 *
 * <p>e1RM is only meaningful for low reps; {@link #isReliable(int)} flags the valid range. Higher-rep
 * sets are excluded from e1RM (but still counted in volume).
 */
public final class EstimatedOneRepMax {

    /** e1RM is only considered reliable at or below this rep count. */
    public static final int MAX_RELIABLE_REPS = 12;

    private EstimatedOneRepMax() {
    }

    /** Epley: {@code w x (1 + reps/30)}. The default estimator. */
    public static double epley(double weightKg, int reps) {
        requirePositiveReps(reps);
        return weightKg * (1 + reps / 30.0);
    }

    /** Brzycki: {@code w x 36 / (37 - reps)}. Valid only for {@code reps < 37}. */
    public static double brzycki(double weightKg, int reps) {
        requirePositiveReps(reps);
        if (reps >= 37) {
            throw new IllegalArgumentException("Brzycki is undefined for reps >= 37: " + reps);
        }
        return weightKg * 36.0 / (37 - reps);
    }

    public static boolean isReliable(int reps) {
        return reps >= 1 && reps <= MAX_RELIABLE_REPS;
    }

    private static void requirePositiveReps(int reps) {
        if (reps < 1) {
            throw new IllegalArgumentException("reps must be >= 1: " + reps);
        }
    }
}
