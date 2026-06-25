package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftlens.domain.ImportSource;
import com.liftlens.ingest.ImportService;
import com.liftlens.insight.InsightDetectionService;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end slice over the Phase 4 REST surface (CLAUDE.md §7) against a crafted history: a rising
 * weighted squat (PROGRESS/PR), a falling bodyweight pull-up (REGRESSION), and one unmapped lift.
 * Covers happy paths, the bodyweight reps trend, write-token auth, and that OpenAPI is served.
 */
@AutoConfigureMockMvc
class ApiEndpointsIT extends AbstractPostgresIT {

    private static final String API_TOKEN = "dev-api-token"; // application.yml default
    private static final String HEADER = "X-API-Token";
    private static final String CSV_HEADER =
            "title,start_time,end_time,description,exercise_title,superset_id,exercise_notes,"
                    + "set_index,set_type,weight_kg,reps,distance_km,duration_seconds,rpe";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ImportService importService;
    @Autowired
    InsightDetectionService detectionService;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void seed() {
        List<String> rows = new ArrayList<>();
        for (int k = 5; k >= 0; k--) {
            LocalDate date = thisMonday.minusWeeks(k);
            String squatWeight = String.valueOf(100 + (5 - k) * 2.5);  // rising -> PROGRESS + PR
            addSets(rows, date, "Squat (Barbell)", squatWeight, 5, 3);
            addSets(rows, date, "Pull Up", "", 7 + k, 3);              // 12..7 falling -> REGRESSION
        }
        // An exercise outside the seed mapping -> stub flagged UNKNOWN (for the unmapped/mapping tests).
        addSets(rows, thisMonday.minusWeeks(5), "Mystery Lift (Cable)", "20", 10, 3);

        byte[] csv = (CSV_HEADER + "\n" + String.join("\n", rows)).getBytes(StandardCharsets.UTF_8);
        importService.importCsv("api-it.csv", csv, ImportSource.CSV);
        detectionService.run();
    }

    /**
     * The mapping test promotes the stub out of {@code UNKNOWN}, so the superclass cleanup (which only
     * deletes UNKNOWN stubs) would otherwise leak it into other test classes and inflate the seeded
     * exercise count. Remove the stub and its dependent rows here, FK-safe.
     */
    @AfterEach
    void removeStubExercise() {
        String byHevy = " WHERE exercise_id IN (SELECT id FROM exercise WHERE hevy_name = 'Mystery Lift (Cable)')";
        jdbc.update("DELETE FROM insight" + byHevy);
        jdbc.update("DELETE FROM exercise_weekly_stat" + byHevy);
        jdbc.update("DELETE FROM exercise_daily_stat" + byHevy);
        jdbc.update("DELETE FROM exercise_set" + byHevy);
        jdbc.update("DELETE FROM exercise WHERE hevy_name = 'Mystery Lift (Cable)'");
    }

    private static void addSets(List<String> rows, LocalDate date, String exercise,
            String weightKg, int reps, int sets) {
        String start = date.format(DAY) + ", 07:00";
        String end = date.format(DAY) + ", 08:00";
        for (int i = 0; i < sets; i++) {
            rows.add("Session,\"%s\",\"%s\",,\"%s\",,,%d,normal,%s,%d,,,"
                    .formatted(start, end, exercise, i, weightKg, reps));
        }
    }

    private long exerciseId(String hevyName) {
        return jdbc.queryForObject("SELECT id FROM exercise WHERE hevy_name = ?", Long.class, hevyName);
    }

    // ---- Reads --------------------------------------------------------------------------------

    @Test
    void listsExercisesWithMappingStatus() throws Exception {
        // Parse in Java rather than JSONPath-filter: Jayway mis-parses parentheses inside the
        // filter string literal (e.g. 'Squat (Barbell)').
        String json = mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> exercises = objectMapper.readValue(json, new TypeReference<>() { });

        assertThat(exercises).filteredOn(e -> "Squat (Barbell)".equals(e.get("hevyName")))
                .singleElement()
                .satisfies(e -> assertThat(e.get("mapped")).isEqualTo(true));
        assertThat(exercises).filteredOn(e -> "Mystery Lift (Cable)".equals(e.get("hevyName")))
                .singleElement()
                .satisfies(e -> assertThat(e.get("mapped")).isEqualTo(false));
    }

    @Test
    void listsUnmappedExercises() throws Exception {
        mockMvc.perform(get("/api/exercises/unmapped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hevyName").value("Mystery Lift (Cable)"))
                .andExpect(jsonPath("$[0].mapped").value(false));
    }

    @Test
    void bodyweightTrendUsesRepsSeriesNotE1rm() throws Exception {
        mockMvc.perform(get("/api/exercises/{id}/trend", exerciseId("Pull Up")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weighted").value(false))
                .andExpect(jsonPath("$.points[0].bestReps").isNumber())
                .andExpect(jsonPath("$.points[0].bestE1rm").doesNotExist());
    }

    @Test
    void weightedTrendUsesE1rmSeries() throws Exception {
        mockMvc.perform(get("/api/exercises/{id}/trend", exerciseId("Squat (Barbell)")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weighted").value(true))
                .andExpect(jsonPath("$.points[0].bestE1rm").isNumber());
    }

    @Test
    void trendForUnknownExerciseIs404() throws Exception {
        mockMvc.perform(get("/api/exercises/{id}/trend", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void dashboardSummaryHasVolumeInsightsAndPrs() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                // weekStart is derived from a UTC clock; assert presence rather than an exact day to
                // stay zone-robust near the Monday boundary.
                .andExpect(jsonPath("$.weekStart").exists())
                .andExpect(jsonPath("$.previousWeekStart").exists())
                .andExpect(jsonPath("$.volumeByMuscle").isArray())
                .andExpect(jsonPath("$.recentPrs").isArray())
                .andExpect(jsonPath("$.activeInsights").isArray());
    }

    @Test
    void listsWorkoutsAndHonoursDateFilter() throws Exception {
        mockMvc.perform(get("/api/workouts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6)); // one session per week

        mockMvc.perform(get("/api/workouts").param("from", thisMonday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)); // only this week's
    }

    @Test
    void listsMuscleWeeklyVolume() throws Exception {
        mockMvc.perform(get("/api/muscles/volume").param("weeks", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].muscle").exists());
    }

    @Test
    void insightFeedShowsActivePullUpRegression() throws Exception {
        mockMvc.perform(get("/api/insights").param("status", "active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'REGRESSION' && @.exerciseId == %d)]"
                        .formatted(exerciseId("Pull Up"))).exists());
    }

    @Test
    void rejectsUnknownInsightStatus() throws Exception {
        mockMvc.perform(get("/api/insights").param("status", "bogus"))
                .andExpect(status().isBadRequest());
    }

    // ---- Writes require the API token --------------------------------------------------------

    @Test
    void importRequiresToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "x.csv",
                MediaType.TEXT_PLAIN_VALUE, (CSV_HEADER + "\n").getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/imports").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dismissRequiresTokenThenDismisses() throws Exception {
        long pullUpId = exerciseId("Pull Up");
        Long insightId = jdbc.queryForObject(
                "SELECT id FROM insight WHERE type = 'REGRESSION' AND exercise_id = ? AND status = 'ACTIVE'",
                Long.class, pullUpId);

        mockMvc.perform(post("/api/insights/{id}/dismiss", insightId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/insights/{id}/dismiss", insightId).header(HEADER, API_TOKEN))
                .andExpect(status().isNoContent());

        String statusValue = jdbc.queryForObject(
                "SELECT status FROM insight WHERE id = ?", String.class, insightId);
        assertThat(statusValue).isEqualTo("DISMISSED");
    }

    @Test
    void dismissUnknownInsightIs404() throws Exception {
        mockMvc.perform(post("/api/insights/{id}/dismiss", 999_999).header(HEADER, API_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void mappingRequiresTokenThenMapsExercise() throws Exception {
        long mysteryId = exerciseId("Mystery Lift (Cable)");
        String body = """
                {"primaryMuscle":"CHEST","movementType":"ISOLATION","secondaryMuscles":["TRICEPS"]}""";

        mockMvc.perform(put("/api/exercises/{id}/mapping", mysteryId)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/exercises/{id}/mapping", mysteryId)
                        .header(HEADER, API_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryMuscle").value("CHEST"))
                .andExpect(jsonPath("$.mapped").value(true));
    }

    @Test
    void mappingRejectsInvalidBody() throws Exception {
        long mysteryId = exerciseId("Mystery Lift (Cable)");
        // missing the required movementType -> 400
        mockMvc.perform(put("/api/exercises/{id}/mapping", mysteryId)
                        .header(HEADER, API_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryMuscle\":\"CHEST\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mappingUnknownExerciseIs404() throws Exception {
        mockMvc.perform(put("/api/exercises/{id}/mapping", 999_999)
                        .header(HEADER, API_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryMuscle\":\"CHEST\",\"movementType\":\"ISOLATION\"}"))
                .andExpect(status().isNotFound());
    }

    // ---- OpenAPI ------------------------------------------------------------------------------

    @Test
    void openApiSpecIsServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/dashboard/summary']").exists())
                .andExpect(jsonPath("$.paths['/api/insights/{id}/dismiss']").exists());
    }
}
