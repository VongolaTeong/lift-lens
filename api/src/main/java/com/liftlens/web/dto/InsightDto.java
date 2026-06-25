package com.liftlens.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An insight for the feed and dashboard cards (CLAUDE.md §7, §8). {@code exerciseName} is joined in
 * for display; {@code metric} is the raw numbers behind the insight (nullable). Either {@code exerciseId}
 * or {@code muscle} is set depending on the detector.
 */
public record InsightDto(
        long id,
        String type,
        String severity,
        Long exerciseId,
        String exerciseName,
        String muscle,
        String title,
        String detail,
        LocalDate windowStart,
        LocalDate windowEnd,
        JsonNode metric,
        Instant detectedAt,
        String status) {
}
