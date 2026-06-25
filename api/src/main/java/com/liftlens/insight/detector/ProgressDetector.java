package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.InsightType;
import com.liftlens.insight.Severity;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.TrendSnapshot;
import org.springframework.stereotype.Component;

/** Strongly positive trend slope → positive reinforcement (CLAUDE.md §5). */
@Component
public class ProgressDetector extends AbstractTrendDetector {

    public ProgressDetector(StatsReadService read, InsightProperties props) {
        super(read, props);
    }

    @Override
    public InsightType type() {
        return InsightType.PROGRESS;
    }

    @Override
    protected DetectedInsight evaluate(TrendSnapshot s) {
        if (s.slope() < progressThreshold(s)) {
            return null;
        }
        return new DetectedInsight(InsightType.PROGRESS, s.exerciseId(), null, Severity.INFO,
                s.exerciseName() + " is progressing",
                "Trending up ~%.3f %s over %d weeks.".formatted(s.slope(), unit(s), s.points()),
                s.windowStart(), s.windowEnd(), metric(s));
    }
}
