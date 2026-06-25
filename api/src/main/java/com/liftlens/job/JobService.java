package com.liftlens.job;

import com.liftlens.insight.InsightDetectionService;
import com.liftlens.stats.PrScanService;
import com.liftlens.stats.StatsRecomputeService;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/**
 * The idempotent jobs (CLAUDE.md §6), exposed as plain methods so both the in-process
 * {@code @Scheduled} triggers and the external-cron HTTP endpoint drive the exact same code.
 */
@Service
public class JobService {

    private final StatsRecomputeService statsRecomputeService;
    private final InsightDetectionService insightDetectionService;
    private final PrScanService prScanService;

    public JobService(StatsRecomputeService statsRecomputeService,
            InsightDetectionService insightDetectionService,
            PrScanService prScanService) {
        this.statsRecomputeService = statsRecomputeService;
        this.insightDetectionService = insightDetectionService;
        this.prScanService = prScanService;
    }

    public void recomputeStats() {
        statsRecomputeService.recomputeAll();
    }

    public void detectInsights() {
        insightDetectionService.run();
    }

    public void prScan() {
        prScanService.scanAll();
    }

    /** Dispatch by the {@code {jobName}} path variable used by the external cron endpoint. */
    public boolean runByName(String jobName) {
        Optional<Job> job = Job.fromName(jobName);
        job.ifPresent(j -> j.action.accept(this));
        return job.isPresent();
    }

    private enum Job {
        RECOMPUTE_STATS("recompute-stats", JobService::recomputeStats),
        DETECT_INSIGHTS("detect-insights", JobService::detectInsights),
        PR_SCAN("pr-scan", JobService::prScan);

        private final String name;
        private final Consumer<JobService> action;

        Job(String name, Consumer<JobService> action) {
            this.name = name;
            this.action = action;
        }

        static Optional<Job> fromName(String name) {
            String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
            for (Job job : values()) {
                if (job.name.equals(normalized)) {
                    return Optional.of(job);
                }
            }
            return Optional.empty();
        }
    }
}
