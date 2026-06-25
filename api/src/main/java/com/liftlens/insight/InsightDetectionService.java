package com.liftlens.insight;

import com.liftlens.insight.InsightUpsertDao.ActiveInsight;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs every {@link InsightDetector}, upserts what they find, and resolves insights whose condition
 * no longer holds (CLAUDE.md §5). Idempotent: a second run over unchanged data upserts the same rows
 * and resolves nothing. New detectors are picked up automatically via constructor injection of the
 * detector list — no change here required.
 */
@Service
public class InsightDetectionService {

    private static final Logger log = LoggerFactory.getLogger(InsightDetectionService.class);

    private final List<InsightDetector> detectors;
    private final InsightUpsertDao dao;
    private final Clock clock;

    public InsightDetectionService(List<InsightDetector> detectors, InsightUpsertDao dao, Clock clock) {
        this.detectors = detectors;
        this.dao = dao;
        this.clock = clock;
    }

    @Transactional
    public InsightRunSummary run() {
        LocalDate referenceDate = LocalDate.now(clock);

        List<DetectedInsight> detected = new ArrayList<>();
        Set<InsightType> evaluatedTypes = EnumSet.noneOf(InsightType.class);
        for (InsightDetector detector : detectors) {
            evaluatedTypes.add(detector.type());
            detected.addAll(detector.detect(referenceDate));
        }

        detected.forEach(dao::upsert);

        Set<Key> activeKeys = detected.stream().map(Key::of).collect(Collectors.toSet());
        List<Long> resolved = dao.findActiveKeys(evaluatedTypes).stream()
                .filter(active -> !activeKeys.contains(Key.of(active)))
                .map(ActiveInsight::id)
                .toList();
        dao.resolve(resolved);

        log.info("Insight run: {} active across {} detector types, {} resolved",
                detected.size(), evaluatedTypes.size(), resolved.size());
        return new InsightRunSummary(detected.size(), resolved.size(), evaluatedTypes.size());
    }

    public record InsightRunSummary(int active, int resolved, int detectorTypes) {
    }

    private record Key(InsightType type, Long exerciseId, String muscle, LocalDate windowEnd) {
        static Key of(DetectedInsight d) {
            return new Key(d.type(), d.exerciseId(), d.muscle(), d.windowEnd());
        }

        static Key of(ActiveInsight a) {
            return new Key(a.type(), a.exerciseId(), a.muscle(), a.windowEnd());
        }
    }
}
