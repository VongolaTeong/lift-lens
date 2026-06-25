package com.liftlens.web;

import com.liftlens.query.ExerciseMappingService;
import com.liftlens.query.ExerciseQueryService;
import com.liftlens.web.dto.ExerciseMappingRequest;
import com.liftlens.web.dto.ExerciseSummaryDto;
import com.liftlens.web.dto.ExerciseTrendDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exercise list/mapping and per-exercise trend (CLAUDE.md §7). The mapping PUT is a write (token). */
@Tag(name = "Exercises")
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseQueryService query;
    private final ExerciseMappingService mapping;

    public ExerciseController(ExerciseQueryService query, ExerciseMappingService mapping) {
        this.query = query;
        this.mapping = mapping;
    }

    @Operation(summary = "List exercises with mapping status and usage")
    @GetMapping
    public List<ExerciseSummaryDto> list() {
        return query.list();
    }

    @Operation(summary = "Exercises still needing a muscle mapping")
    @GetMapping("/unmapped")
    public List<ExerciseSummaryDto> unmapped() {
        return query.unmapped();
    }

    @Operation(summary = "e1RM + volume series (or reps series for bodyweight) for charts")
    @GetMapping("/{id}/trend")
    public ExerciseTrendDto trend(@PathVariable long id,
            @RequestParam(defaultValue = "12") int weeks) {
        return query.trend(id, weeks);
    }

    @Operation(summary = "Assign a muscle/movement mapping to an exercise")
    @PutMapping("/{id}/mapping")
    public ExerciseSummaryDto updateMapping(@PathVariable long id,
            @Valid @RequestBody ExerciseMappingRequest request) {
        mapping.applyMapping(id, request);
        return query.getById(id);
    }
}
