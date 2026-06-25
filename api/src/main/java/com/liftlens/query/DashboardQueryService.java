package com.liftlens.query;

import com.liftlens.web.dto.DashboardSummaryDto;
import com.liftlens.web.dto.InsightDto;
import com.liftlens.web.dto.MuscleVolumeComparisonDto;
import com.liftlens.web.dto.PrDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Assembles the dashboard snapshot (CLAUDE.md §7, §8): this-week vs last-week working volume by muscle,
 * the active insight cards, and recent PRs. Weeks are ISO-week Mondays from the current clock.
 */
@Service
public class DashboardQueryService {

    private static final int RECENT_PR_WEEKS = 4;
    private static final int MAX_DASHBOARD_INSIGHTS = 20;

    private final NamedParameterJdbcTemplate jdbc;
    private final InsightQueryService insightQueryService;
    private final Clock clock;

    public DashboardQueryService(NamedParameterJdbcTemplate jdbc,
            InsightQueryService insightQueryService, Clock clock) {
        this.jdbc = jdbc;
        this.insightQueryService = insightQueryService;
        this.clock = clock;
    }

    public DashboardSummaryDto summary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = thisMonday.minusWeeks(1);

        List<MuscleVolumeComparisonDto> volumeByMuscle = volumeComparison(thisMonday, lastMonday);
        List<PrDto> recentPrs = recentPrs(today.minusWeeks(RECENT_PR_WEEKS));
        List<InsightDto> activeInsights = insightQueryService.list("active").stream()
                .limit(MAX_DASHBOARD_INSIGHTS)
                .toList();

        return new DashboardSummaryDto(thisMonday, lastMonday, volumeByMuscle, activeInsights, recentPrs);
    }

    private List<MuscleVolumeComparisonDto> volumeComparison(LocalDate thisWeek, LocalDate lastWeek) {
        Map<String, long[]> sets = new LinkedHashMap<>();          // muscle -> [thisSets, lastSets]
        Map<String, BigDecimal[]> volume = new LinkedHashMap<>();  // muscle -> [thisVol, lastVol]

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("thisWeek", thisWeek)
                .addValue("lastWeek", lastWeek);
        jdbc.query("""
                SELECT muscle,
                       to_date(iso_year || '-' || lpad(iso_week::text, 2, '0'), 'IYYY-IW') AS week_start,
                       working_volume, set_count
                FROM muscle_weekly_volume
                WHERE to_date(iso_year || '-' || lpad(iso_week::text, 2, '0'), 'IYYY-IW')
                      IN (:thisWeek, :lastWeek)
                """, params, rs -> {
            String muscle = rs.getString("muscle");
            LocalDate weekStart = rs.getObject("week_start", LocalDate.class);
            boolean current = thisWeek.equals(weekStart);
            BigDecimal vol = rs.getBigDecimal("working_volume");
            int setCount = rs.getInt("set_count");

            BigDecimal[] v = volume.computeIfAbsent(muscle, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            long[] s = sets.computeIfAbsent(muscle, k -> new long[2]);
            v[current ? 0 : 1] = vol == null ? BigDecimal.ZERO : vol;
            s[current ? 0 : 1] = setCount;
        });

        return volume.keySet().stream()
                .map(muscle -> new MuscleVolumeComparisonDto(
                        muscle,
                        volume.get(muscle)[0], volume.get(muscle)[1],
                        (int) sets.get(muscle)[0], (int) sets.get(muscle)[1]))
                .sorted(Comparator.comparing(MuscleVolumeComparisonDto::thisWeekVolume).reversed()
                        .thenComparing(MuscleVolumeComparisonDto::muscle))
                .toList();
    }

    private List<PrDto> recentPrs(LocalDate since) {
        return jdbc.query("""
                SELECT es.exercise_id, e.canonical_name AS name,
                       (w.started_at AT TIME ZONE 'UTC')::date AS d,
                       es.load_basis, es.weight_kg, es.reps,
                       CASE WHEN es.weight_kg IS NOT NULL AND es.reps <= 12
                            THEN es.weight_kg * (1 + es.reps / 30.0) END AS e1rm,
                       CASE WHEN es.weight_kg IS NOT NULL THEN es.weight_kg * es.reps END AS vol
                FROM exercise_set es
                JOIN workout w ON w.id = es.workout_id
                JOIN exercise e ON e.id = es.exercise_id
                WHERE es.is_pr AND (w.started_at AT TIME ZONE 'UTC')::date >= :since
                ORDER BY d DESC, es.exercise_id
                LIMIT 10
                """, new MapSqlParameterSource("since", since), (rs, n) -> new PrDto(
                rs.getLong("exercise_id"),
                rs.getString("name"),
                rs.getObject("d", LocalDate.class),
                rs.getString("load_basis"),
                rs.getBigDecimal("weight_kg"),
                (Integer) rs.getObject("reps"),
                rs.getBigDecimal("e1rm"),
                rs.getBigDecimal("vol")));
    }
}
