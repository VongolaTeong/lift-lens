package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightDetector;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.InsightType;
import com.liftlens.insight.Severity;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.MuscleWeekPoint;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** A trained muscle below a minimum set count over the recent window (CLAUDE.md §5). */
@Component
public class NeglectDetector implements InsightDetector {

    private final StatsReadService read;
    private final InsightProperties props;

    public NeglectDetector(StatsReadService read, InsightProperties props) {
        this.read = read;
        this.props = props;
    }

    @Override
    public InsightType type() {
        return InsightType.NEGLECT;
    }

    @Override
    public List<DetectedInsight> detect(LocalDate referenceDate) {
        LocalDate windowEnd = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate windowStart = windowEnd.minusWeeks(props.neglectWeeks() - 1L);

        Map<String, Integer> recentSets = new LinkedHashMap<>();
        for (MuscleWeekPoint p : read.muscleWeeklyPoints()) {
            if (!p.weekStart().isBefore(windowStart)) {
                recentSets.merge(p.muscle(), p.setCount(), Integer::sum);
            }
        }

        List<DetectedInsight> out = new ArrayList<>();
        for (String muscle : read.trainedMuscles()) {
            int sets = recentSets.getOrDefault(muscle, 0);
            if (sets >= props.neglectMinSets()) {
                continue;
            }
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("setsInWindow", sets);
            metric.put("minSets", props.neglectMinSets());
            metric.put("weeks", props.neglectWeeks());
            out.add(new DetectedInsight(InsightType.NEGLECT, null, muscle, Severity.WARN,
                    muscle + " is neglected",
                    "Only %d working sets in the last %d weeks (target ≥ %d)."
                            .formatted(sets, props.neglectWeeks(), props.neglectMinSets()),
                    windowStart, windowEnd, metric));
        }
        return out;
    }
}
