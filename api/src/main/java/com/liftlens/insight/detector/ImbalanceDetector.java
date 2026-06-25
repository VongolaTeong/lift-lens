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

/**
 * Antagonist/related muscle set-count ratio outside the target band (CLAUDE.md §5).
 *
 * <p>Uses set counts rather than tonnage so bodyweight pulling (Pull Up etc., ~0 kg volume) isn't
 * unfairly counted as "missing" against weighted pushing.
 */
@Component
public class ImbalanceDetector implements InsightDetector {

    private static final List<String[]> PAIRS = List.of(
            new String[] {"CHEST", "BACK"},
            new String[] {"BICEPS", "TRICEPS"},
            new String[] {"QUADS", "HAMSTRINGS"});

    private final StatsReadService read;
    private final InsightProperties props;

    public ImbalanceDetector(StatsReadService read, InsightProperties props) {
        this.read = read;
        this.props = props;
    }

    @Override
    public InsightType type() {
        return InsightType.IMBALANCE;
    }

    @Override
    public List<DetectedInsight> detect(LocalDate referenceDate) {
        LocalDate windowEnd = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate windowStart = windowEnd.minusWeeks(props.imbalanceWindowWeeks() - 1L);

        Map<String, Integer> sets = new LinkedHashMap<>();
        for (MuscleWeekPoint p : read.muscleWeeklyPoints()) {
            if (!p.weekStart().isBefore(windowStart)) {
                sets.merge(p.muscle(), p.setCount(), Integer::sum);
            }
        }

        List<DetectedInsight> out = new ArrayList<>();
        for (String[] pair : PAIRS) {
            int a = sets.getOrDefault(pair[0], 0);
            int b = sets.getOrDefault(pair[1], 0);
            int high = Math.max(a, b);
            int low = Math.min(a, b);
            if (high < props.imbalanceMinSets()) {
                continue; // too little work either side to judge
            }
            double ratio = low == 0 ? Double.POSITIVE_INFINITY : (double) high / low;
            if (ratio < props.imbalanceRatio()) {
                continue;
            }
            String under = a < b ? pair[0] : pair[1];
            String over = a < b ? pair[1] : pair[0];
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put(over, high);
            metric.put(under, low);
            metric.put("ratio", low == 0 ? null : Math.round(ratio * 100.0) / 100.0);
            metric.put("weeks", props.imbalanceWindowWeeks());
            out.add(new DetectedInsight(InsightType.IMBALANCE, null, under, Severity.WARN,
                    under + " undertrained vs " + over,
                    "%s %d sets vs %s %d sets over %d weeks."
                            .formatted(over, high, under, low, props.imbalanceWindowWeeks()),
                    windowStart, windowEnd, metric));
        }
        return out;
    }
}
