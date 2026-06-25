package com.liftlens.insight;

import java.time.LocalDate;
import java.util.List;

/**
 * One detection strategy (CLAUDE.md §5). Detectors are independent and order-free: each reads
 * materialized stats and returns the insights that currently hold. Adding a detector requires no
 * change to the others — the orchestrator collects every {@code InsightDetector} bean.
 */
public interface InsightDetector {

    /** The single insight type this detector owns (drives the resolve scope). */
    InsightType type();

    /** Insights holding as of {@code referenceDate}; an empty list means the condition cleared. */
    List<DetectedInsight> detect(LocalDate referenceDate);
}
