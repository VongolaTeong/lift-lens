package com.liftlens;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for integration tests: one shared Postgres 16 container for the whole suite (singleton
 * pattern), wired into Spring Boot via {@code @ServiceConnection}. Each test starts from a clean
 * slate — fact tables emptied and any unmapped exercise stubs removed, leaving the 40 seeded rows.
 */
@SpringBootTest
abstract class AbstractPostgresIT {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetFactTables() {
        jdbcTemplate.execute("DELETE FROM exercise_set");
        jdbcTemplate.execute("DELETE FROM workout");
        jdbcTemplate.execute("DELETE FROM import_batch");
        jdbcTemplate.execute("DELETE FROM exercise WHERE primary_muscle = 'UNKNOWN'");
    }
}
