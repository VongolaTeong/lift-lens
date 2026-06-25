package com.liftlens.query;

import com.liftlens.web.dto.MuscleVolumePointDto;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read side for the muscle-balance view ({@code GET /api/muscles/volume?weeks=}). Returns the per-muscle
 * weekly working-volume points over the trailing {@code weeks} window, ordered for the UI to group.
 */
@Service
public class MuscleQueryService {

    private static final String SELECT = """
            SELECT muscle, iso_year, iso_week,
                   to_date(iso_year || '-' || lpad(iso_week::text, 2, '0'), 'IYYY-IW') AS week_start,
                   working_volume, set_count, session_count
            FROM muscle_weekly_volume
            WHERE to_date(iso_year || '-' || lpad(iso_week::text, 2, '0'), 'IYYY-IW') >= :cutoff
            ORDER BY muscle, iso_year, iso_week
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public MuscleQueryService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public List<MuscleVolumePointDto> weeklyVolume(int weeks) {
        int window = Math.max(1, weeks);
        LocalDate currentMonday = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate cutoff = currentMonday.minusWeeks(window - 1L);

        return jdbc.query(SELECT, new MapSqlParameterSource("cutoff", cutoff),
                (rs, n) -> new MuscleVolumePointDto(
                        rs.getString("muscle"),
                        rs.getObject("week_start", LocalDate.class),
                        rs.getInt("iso_year"),
                        rs.getInt("iso_week"),
                        rs.getBigDecimal("working_volume"),
                        rs.getInt("set_count"),
                        rs.getInt("session_count")));
    }
}
