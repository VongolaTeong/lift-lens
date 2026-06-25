package com.liftlens.insight.detector;

import com.liftlens.insight.DetectedInsight;
import com.liftlens.insight.InsightDetector;
import com.liftlens.insight.InsightProperties;
import com.liftlens.insight.InsightType;
import com.liftlens.insight.Severity;
import com.liftlens.insight.StatsReadService;
import com.liftlens.insight.StatsReadService.ExerciseHistory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** An exercise once trained regularly, now lapsed beyond a session threshold (CLAUDE.md §5). */
@Component
public class DropoffDetector implements InsightDetector {

    private final StatsReadService read;
    private final InsightProperties props;

    public DropoffDetector(StatsReadService read, InsightProperties props) {
        this.read = read;
        this.props = props;
    }

    @Override
    public InsightType type() {
        return InsightType.DROPOFF;
    }

    @Override
    public List<DetectedInsight> detect(LocalDate referenceDate) {
        LocalDate windowEnd = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DetectedInsight> out = new ArrayList<>();
        for (ExerciseHistory h : read.exerciseHistories()) {
            if (h.sessions() < props.dropoffMinHistorySessions() || h.lastSession() == null) {
                continue;
            }
            long weeksSince = ChronoUnit.WEEKS.between(h.lastSession(), referenceDate);
            if (weeksSince <= props.dropoffLapseWeeks()) {
                continue;
            }
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("lastSession", h.lastSession().toString());
            metric.put("weeksSince", weeksSince);
            metric.put("lifetimeSessions", h.sessions());
            out.add(new DetectedInsight(InsightType.DROPOFF, h.exerciseId(), null, Severity.WARN,
                    h.exerciseName() + " dropped off",
                    "Last trained %d weeks ago after %d sessions."
                            .formatted(weeksSince, h.sessions()),
                    h.lastSession(), windowEnd, metric));
        }
        return out;
    }
}
