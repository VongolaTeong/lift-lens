package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.InsightType;
import com.liftlens.insight.Severity;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.TrendSnapshot;
import org.springframework.stereotype.Component;

/** Near-flat trend slope over enough data points → a plateau (CLAUDE.md §5). */
@Component
public class PlateauDetector extends AbstractTrendDetector {

    public PlateauDetector(StatsReadService read, InsightProperties props) {
        super(read, props);
    }

    @Override
    public InsightType type() {
        return InsightType.PLATEAU;
    }

    @Override
    protected DetectedInsight evaluate(TrendSnapshot s) {
        if (Math.abs(s.slope()) > plateauBand(s)) {
            return null;
        }
        return new DetectedInsight(InsightType.PLATEAU, s.exerciseId(), null, Severity.INFO,
                s.exerciseName() + " has plateaued",
                "Flat (~%.3f %s) over %d weeks.".formatted(s.slope(), unit(s), s.points()),
                s.windowStart(), s.windowEnd(), metric(s));
    }
}
