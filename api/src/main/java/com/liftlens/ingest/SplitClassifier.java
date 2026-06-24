package com.liftlens.ingest;

import com.liftlens.domain.SplitCategory;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Derives a workout's {@link SplitCategory} by majority vote of its exercises' primary muscles
 * (CLAUDE.md §4). Title is never trusted — {@code "Push pull"} sessions are genuinely mixed and
 * resolve to {@link SplitCategory#UPPER} here, not by parsing the name.
 *
 * <p>Input is one primary-muscle entry per distinct exercise in the workout.
 */
public final class SplitClassifier {

    private static final Set<String> PUSH = Set.of("CHEST", "SHOULDERS", "TRICEPS");
    private static final Set<String> PULL = Set.of("BACK", "REAR_DELTS", "TRAPS", "BICEPS", "FOREARMS");
    private static final Set<String> LOWER =
            Set.of("QUADS", "HAMSTRINGS", "GLUTES", "ABDUCTORS", "ADDUCTORS", "CALVES");

    // A side is "genuinely mixed" (→ UPPER) when the smaller of push/pull is at least this share.
    private static final int MIXED_THRESHOLD_PERCENT = 30;

    private SplitClassifier() {
    }

    public static SplitCategory classify(Collection<String> primaryMusclesPerExercise) {
        int push = 0;
        int pull = 0;
        int lower = 0;
        int other = 0;
        for (String muscle : primaryMusclesPerExercise) {
            String key = muscle == null ? "" : muscle.toUpperCase(Locale.ROOT);
            if (PUSH.contains(key)) {
                push++;
            } else if (PULL.contains(key)) {
                pull++;
            } else if (LOWER.contains(key)) {
                lower++;
            } else {
                other++;
            }
        }

        int total = push + pull + lower + other;
        if (total == 0) {
            return SplitCategory.OTHER;
        }
        // Leg-dominant session.
        if (lower * 2 >= total && lower >= push && lower >= pull) {
            return SplitCategory.LOWER;
        }
        int upper = push + pull;
        if (upper == 0) {
            return SplitCategory.OTHER;
        }
        if (push > 0 && pull > 0) {
            int min = Math.min(push, pull);
            if (min * 100 >= MIXED_THRESHOLD_PERCENT * upper) {
                return SplitCategory.UPPER;
            }
            return push >= pull ? SplitCategory.PUSH : SplitCategory.PULL;
        }
        return push > 0 ? SplitCategory.PUSH : SplitCategory.PULL;
    }
}
