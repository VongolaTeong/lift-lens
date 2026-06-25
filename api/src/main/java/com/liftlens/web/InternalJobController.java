package com.liftlens.web;

import com.liftlens.job.JobService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External-cron entry point for the jobs (CLAUDE.md §6). Token-protected by {@code InternalTokenFilter}
 * at the security layer; here we only dispatch by job name.
 */
@RestController
@RequestMapping("/internal/jobs")
public class InternalJobController {

    private final JobService jobs;

    public InternalJobController(JobService jobs) {
        this.jobs = jobs;
    }

    @PostMapping("/{jobName}")
    public ResponseEntity<Map<String, String>> run(@PathVariable String jobName) {
        boolean ran = jobs.runByName(jobName);
        if (!ran) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("job", jobName, "status", "OK"));
    }
}
