package com.liftlens.insight;

import java.time.LocalDate;
import java.util.Map;

/**
 * An insight a detector found this run, before persistence. The natural key
 * (type, exerciseId, muscle, windowEnd) drives upsert + resolve (CLAUDE.md §5).
 */
public record DetectedInsight(
        InsightType type,
        Long exerciseId,
        String muscle,
        Severity severity,
        String title,
        String detail,
        LocalDate windowStart,
        LocalDate windowEnd,
        Map<String, Object> metric) {
}
