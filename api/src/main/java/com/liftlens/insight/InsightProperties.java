package com.liftlens.insight;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tunable detector thresholds (CLAUDE.md §5 — "tune thresholds in config"). Bound from
 * {@code liftlens.insights.*}; every field has a sensible default so the app runs without config.
 *
 * <p>Weighted lifts trend on e1RM (kg/week); bodyweight lifts trend on reps (reps/week) — hence the
 * separate slope thresholds.
 */
@ConfigurationProperties(prefix = "liftlens.insights")
public record InsightProperties(

        @DefaultValue("4") int trendMinPoints,
        @DefaultValue("12") int trendMaxStaleWeeks,

        @DefaultValue("-0.5") double e1rmRegressionSlope,
        @DefaultValue("0.5") double e1rmProgressSlope,
        @DefaultValue("0.25") double e1rmPlateauBand,

        @DefaultValue("-0.15") double repsRegressionSlope,
        @DefaultValue("0.15") double repsProgressSlope,
        @DefaultValue("0.05") double repsPlateauBand,

        @DefaultValue("4") int neglectWeeks,
        @DefaultValue("8") int neglectMinSets,

        @DefaultValue("6") int dropoffMinHistorySessions,
        @DefaultValue("4") int dropoffLapseWeeks,

        @DefaultValue("4") int imbalanceWindowWeeks,
        @DefaultValue("2.0") double imbalanceRatio,
        @DefaultValue("6") int imbalanceMinSets,

        @DefaultValue("4") int prRecentWeeks) {
}
