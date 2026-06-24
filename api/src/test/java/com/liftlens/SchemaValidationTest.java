package com.liftlens;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full Spring context against a throwaway Postgres 16 container: Flyway must migrate
 * the baseline + seed, and Hibernate's {@code ddl-auto=validate} must agree with the schema.
 * This is the Phase 0 "green Testcontainers harness" acceptance check.
 */
@SpringBootTest
@Testcontainers
class SchemaValidationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayMigratesBaselineAndSeed() {
        // All eight tables from the baseline exist.
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

        // Curated seed: nothing should be left UNKNOWN for the known export.
        Integer unmapped = jdbc.queryForObject(
                "SELECT count(*) FROM exercise WHERE primary_muscle = 'UNKNOWN'", Integer.class);
        assertThat(unmapped).isZero();

        // Bodyweight movements are flagged via equipment (Pull Up — the REGRESSION example).
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
