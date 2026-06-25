package com.liftlens.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-process schedule for always-on hosts (CLAUDE.md §6). On free tiers that idle, these may not
 * fire — the token-protected {@code POST /internal/jobs/{name}} endpoint, driven by an external cron,
 * runs the same {@link JobService} methods. Both paths are safe to fire redundantly (idempotent).
 */
@Component
public class ScheduledJobs {

    private final JobService jobs;

    public ScheduledJobs(JobService jobs) {
        this.jobs = jobs;
    }

    /** Nightly stats refresh at 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    void nightlyRecompute() {
        jobs.recomputeStats();
    }

    /** Weekly insight detection, Sunday 22:00. */
    @Scheduled(cron = "0 0 22 * * SUN")
    void weeklyInsightDetection() {
        jobs.detectInsights();
    }
}
