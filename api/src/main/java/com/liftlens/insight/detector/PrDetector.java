package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightDetector;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.InsightType;
import com.liftlens.insight.Severity;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.PrRef;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Surfaces the most recent PR per exercise from the {@code is_pr} flags set in Phase 2 (CLAUDE.md §5). */
@Component
public class PrDetector implements InsightDetector {

    private final StatsReadService read;
    private final InsightProperties props;

    public PrDetector(StatsReadService read, InsightProperties props) {
        this.read = read;
        this.props = props;
    }

    @Override
    public InsightType type() {
        return InsightType.PR;
    }

    @Override
    public List<DetectedInsight> detect(LocalDate referenceDate) {
        LocalDate windowEnd = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate since = windowEnd.minusWeeks(props.prRecentWeeks() - 1L);

        List<DetectedInsight> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (PrRef pr : read.prsSince(since)) {
            if (!seen.add(pr.exerciseId())) {
                continue; // prsSince is newest-first per exercise → keep the latest only
            }
            LocalDate prWeek = pr.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            boolean weighted = "WEIGHTED".equals(pr.loadBasis());

            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("date", pr.date().toString());
            metric.put("basis", pr.loadBasis());
            metric.put("reps", pr.reps());
            if (weighted) {
                metric.put("weightKg", pr.weightKg());
                metric.put("e1rm", pr.e1rm());
                metric.put("volume", pr.volume());
            }
            String detail = weighted
                    ? "Top set %s kg × %d.".formatted(pr.weightKg(), pr.reps())
                    : "%d reps (bodyweight).".formatted(pr.reps());
            out.add(new DetectedInsight(InsightType.PR, pr.exerciseId(), null, Severity.INFO,
                    "New PR: " + pr.exerciseName(), detail, prWeek, prWeek, metric));
        }
        return out;
    }
}
