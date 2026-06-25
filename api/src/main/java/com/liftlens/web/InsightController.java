package com.liftlens.web;

import com.liftlens.query.InsightQueryService;
import com.liftlens.web.dto.InsightDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Insight feed and dismissal (CLAUDE.md §7). Dismiss is a write (token). */
@Tag(name = "Insights")
@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightQueryService insights;

    public InsightController(InsightQueryService insights) {
        this.insights = insights;
    }

    @Operation(summary = "Insight feed; status = active (default) | dismissed | resolved | all")
    @GetMapping
    public List<InsightDto> list(@RequestParam(defaultValue = "active") String status) {
        return insights.list(status);
    }

    @Operation(summary = "Dismiss an insight")
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable long id) {
        insights.dismiss(id);
        return ResponseEntity.noContent().build();
    }
}
