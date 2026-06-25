package com.liftlens.insight;

/** Categories of insight the detectors produce. Matches the {@code insight.type} CHECK (CLAUDE.md §5). */
public enum InsightType {
    PLATEAU,
    REGRESSION,
    PROGRESS,
    IMBALANCE,
    NEGLECT,
    DROPOFF,
    PR
}
