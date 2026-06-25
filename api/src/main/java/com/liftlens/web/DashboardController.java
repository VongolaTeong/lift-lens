package com.liftlens.web;

import com.liftlens.query.DashboardQueryService;
import com.liftlens.web.dto.DashboardSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** This-week dashboard snapshot (CLAUDE.md §7). */
@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardQueryService dashboard;

    public DashboardController(DashboardQueryService dashboard) {
        this.dashboard = dashboard;
    }

    @Operation(summary = "This-week vs last-week volume by muscle, active insights, and recent PRs")
    @GetMapping("/summary")
    public DashboardSummaryDto summary() {
        return dashboard.summary();
    }
}
