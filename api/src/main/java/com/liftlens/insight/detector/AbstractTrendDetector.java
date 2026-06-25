package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightDetector;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.TrendSnapshot;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared scaffolding for the slope-based trend detectors (PLATEAU / REGRESSION / PROGRESS). Applies
 * the common gates — enough data points, a usable slope, and recent-enough data — then defers the
 * threshold decision to the subclass. Keeps the three detectors independent (each is its own bean).
 */
abstract class AbstractTrendDetector implements InsightDetector {

    protected final StatsReadService read;
    protected final InsightProperties props;

    protected AbstractTrendDetector(StatsReadService read, InsightProperties props) {
        this.read = read;
        this.props = props;
    }

    @Override
    public List<DetectedInsight> detect(LocalDate referenceDate) {
        List<DetectedInsight> out = new ArrayList<>();
        for (TrendSnapshot s : read.trendSnapshots()) {
            if (s.points() < props.trendMinPoints() || Double.isNaN(s.slope())) {
                continue;
            }
            if (ChronoUnit.WEEKS.between(s.windowEnd(), referenceDate) > props.trendMaxStaleWeeks()) {
                continue; // stale exercise — DROPOFF handles lapses, not the trend detectors
            }
            DetectedInsight insight = evaluate(s);
            if (insight != null) {
                out.add(insight);
            }
        }
        return out;
    }

    /** @return an insight if this detector's condition holds for the snapshot, else null. */
    protected abstract DetectedInsight evaluate(TrendSnapshot s);

    protected double regressionThreshold(TrendSnapshot s) {
        return s.weighted() ? props.e1rmRegressionSlope() : props.repsRegressionSlope();
    }

    protected double progressThreshold(TrendSnapshot s) {
        return s.weighted() ? props.e1rmProgressSlope() : props.repsProgressSlope();
    }

    protected double plateauBand(TrendSnapshot s) {
        return s.weighted() ? props.e1rmPlateauBand() : props.repsPlateauBand();
    }

    protected String unit(TrendSnapshot s) {
        return s.weighted() ? "kg/week (e1RM)" : "reps/week";
    }

    protected Map<String, Object> metric(TrendSnapshot s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("slope", round(s.slope()));
        m.put("basis", s.weighted() ? "WEIGHTED" : "BODYWEIGHT");
        m.put("points", s.points());
        m.put("unit", unit(s));
        return m;
    }

    protected static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
