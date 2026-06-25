package com.liftlens.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The dashboard snapshot (CLAUDE.md §7, §8): this-week vs last-week volume by muscle, the active
 * insight cards, and recent PRs. Week starts are ISO-week Mondays derived from the current date.
 */
public record DashboardSummaryDto(
        LocalDate weekStart,
        LocalDate previousWeekStart,
        List<MuscleVolumeComparisonDto> volumeByMuscle,
        List<InsightDto> activeInsights,
        List<PrDto> recentPrs) {
}
