package com.liftlens.web;

import com.liftlens.query.MuscleQueryService;
import com.liftlens.web.dto.MuscleVolumePointDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Per-muscle weekly volume for the balance view (CLAUDE.md §7). */
@Tag(name = "Muscles")
@RestController
@RequestMapping("/api/muscles")
public class MuscleController {

    private final MuscleQueryService muscles;

    public MuscleController(MuscleQueryService muscles) {
        this.muscles = muscles;
    }

    @Operation(summary = "Weekly working volume per muscle over the trailing window")
    @GetMapping("/volume")
    public List<MuscleVolumePointDto> volume(@RequestParam(defaultValue = "12") int weeks) {
        return muscles.weeklyVolume(weeks);
    }
}
