package com.liftlens.web;

import com.liftlens.query.WorkoutQueryService;
import com.liftlens.web.dto.WorkoutSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Session list with optional date bounds (CLAUDE.md §7). */
@Tag(name = "Workouts")
@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutQueryService workouts;

    public WorkoutController(WorkoutQueryService workouts) {
        this.workouts = workouts;
    }

    @Operation(summary = "List sessions, newest first, optionally bounded by from/to (ISO dates)")
    @GetMapping
    public List<WorkoutSummaryDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return workouts.list(from, to);
    }
}
