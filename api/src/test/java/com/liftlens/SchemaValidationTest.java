package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Phase 0 acceptance check: Flyway migrates the baseline + seed against a real Postgres 16, and
 * Hibernate's {@code ddl-auto=validate} agrees with the schema (the context boots via the base).
 */
class SchemaValidationTest extends AbstractPostgresIT {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayMigratesBaselineAndSeed() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);
        assertThat(tables).contains(
                "exercise", "import_batch", "workout", "exercise_set",
                "exercise_daily_stat", "exercise_weekly_stat", "muscle_weekly_volume", "insight");
    }

    @Test
    void seedLoadsAll40ExercisesWithMuscleMapping() {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM exercise", Integer.class);
        assertThat(total).isEqualTo(40);

        Integer unmapped = jdbc.queryForObject(
                "SELECT count(*) FROM exercise WHERE primary_muscle = 'UNKNOWN'", Integer.class);
        assertThat(unmapped).isZero();

        String pullUpEquipment = jdbc.queryForObject(
                "SELECT equipment FROM exercise WHERE hevy_name = 'Pull Up'", String.class);
        assertThat(pullUpEquipment).isEqualTo("BODYWEIGHT");
    }

    @Test
    void factTablesStartEmpty() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM workout", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM exercise_set", Integer.class)).isZero();
    }
}
