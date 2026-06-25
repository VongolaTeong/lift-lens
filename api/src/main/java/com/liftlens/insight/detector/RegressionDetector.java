package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.InsightType;
import com.liftlens.insight.Severity;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.TrendSnapshot;
import org.springframework.stereotype.Component;

/**
 * Significantly negative trend slope (CLAUDE.md §5). This is the named Pull Up example: on the
 * bodyweight reps series, declining reps surface here.
 */
@Component
public class RegressionDetector extends AbstractTrendDetector {

    public RegressionDetector(StatsReadService read, InsightProperties props) {
        super(read, props);
    }

    @Override
    public InsightType type() {
        return InsightType.REGRESSION;
    }

    @Override
    protected DetectedInsight evaluate(TrendSnapshot s) {
        double threshold = regressionThreshold(s);
        if (s.slope() > threshold) {
            return null;
        }
        Severity severity = s.slope() <= 2 * threshold ? Severity.HIGH : Severity.WARN;
        return new DetectedInsight(InsightType.REGRESSION, s.exerciseId(), null, severity,
                s.exerciseName() + " is regressing",
                "Trending down ~%.3f %s over %d weeks.".formatted(s.slope(), unit(s), s.points()),
                s.windowStart(), s.windowEnd(), metric(s));
    }
}
