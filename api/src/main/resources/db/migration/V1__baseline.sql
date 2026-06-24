-- V1 — LiftLens schema baseline (CLAUDE.md §3).
-- Enums modeled as TEXT + CHECK for portability and easy evolution under Flyway.

-- ============================================================
-- Reference / dimension
-- ============================================================
CREATE TABLE exercise (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hevy_name         TEXT NOT NULL UNIQUE,                 -- raw Hevy exercise_title
    canonical_name    TEXT NOT NULL,
    primary_muscle    TEXT NOT NULL,                        -- 'UNKNOWN' until mapped
    secondary_muscles JSONB NOT NULL DEFAULT '[]'::jsonb,
    equipment         TEXT,
    movement_type     TEXT NOT NULL DEFAULT 'UNKNOWN'
                        CHECK (movement_type IN ('COMPOUND', 'ISOLATION', 'UNKNOWN')),
    is_unilateral     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- Ingested / fact
-- ============================================================
CREATE TABLE import_batch (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source      TEXT NOT NULL CHECK (source IN ('CSV', 'API')),
    filename    TEXT,
    checksum    TEXT NOT NULL UNIQUE,                       -- sha256 of file; reject exact dupes
    row_count   INTEGER NOT NULL DEFAULT 0,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status      TEXT NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE workout (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hevy_natural_key TEXT NOT NULL UNIQUE,                  -- hash(title + start_time)
    title            TEXT NOT NULL,
    started_at       TIMESTAMPTZ NOT NULL,
    ended_at         TIMESTAMPTZ,
    duration_seconds INTEGER,
    split_category   TEXT NOT NULL DEFAULT 'OTHER'
                       CHECK (split_category IN ('PUSH', 'PULL', 'UPPER', 'LOWER', 'OTHER')),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE exercise_set (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workout_id       BIGINT NOT NULL REFERENCES workout (id),
    exercise_id      BIGINT NOT NULL REFERENCES exercise (id),
    set_index        INTEGER NOT NULL,                      -- 0-based per exercise within a workout
    set_type         TEXT NOT NULL DEFAULT 'normal',
    weight_kg        NUMERIC(7, 2),                         -- NULL for bodyweight movements
    reps             INTEGER,
    rpe              NUMERIC(4, 2),
    distance_m       NUMERIC(10, 2),
    duration_seconds INTEGER,
    is_working       BOOLEAN NOT NULL DEFAULT TRUE,         -- not warmup
    load_basis       TEXT NOT NULL DEFAULT 'WEIGHTED'       -- drives weighted vs reps progression (§5)
                       CHECK (load_basis IN ('WEIGHTED', 'BODYWEIGHT')),
    is_pr            BOOLEAN NOT NULL DEFAULT FALSE,        -- set by analytics
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_exercise_set UNIQUE (workout_id, exercise_id, set_index)
);

-- ============================================================
-- Derived / materialized (written only by scheduled jobs — §6)
-- ============================================================
CREATE TABLE exercise_daily_stat (
    exercise_id      BIGINT NOT NULL REFERENCES exercise (id),
    stat_date        DATE NOT NULL,
    top_working_e1rm NUMERIC(8, 2),
    working_volume   NUMERIC(12, 2),
    working_sets     INTEGER NOT NULL DEFAULT 0,
    max_weight       NUMERIC(7, 2),
    total_reps       INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (exercise_id, stat_date)
);

CREATE TABLE exercise_weekly_stat (
    exercise_id BIGINT NOT NULL REFERENCES exercise (id),
    iso_year    INTEGER NOT NULL,
    iso_week    INTEGER NOT NULL,
    best_e1rm   NUMERIC(8, 2),
    volume      NUMERIC(12, 2),
    sets        INTEGER NOT NULL DEFAULT 0,
    sessions    INTEGER NOT NULL DEFAULT 0,
    e1rm_slope  NUMERIC(10, 4),                             -- trailing-window regression slope
    PRIMARY KEY (exercise_id, iso_year, iso_week)
);

CREATE TABLE muscle_weekly_volume (
    muscle         TEXT NOT NULL,
    iso_year       INTEGER NOT NULL,
    iso_week       INTEGER NOT NULL,
    working_volume NUMERIC(12, 2) NOT NULL DEFAULT 0,
    set_count      INTEGER NOT NULL DEFAULT 0,
    session_count  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (muscle, iso_year, iso_week)
);

CREATE TABLE insight (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type         TEXT NOT NULL
                   CHECK (type IN ('PLATEAU', 'REGRESSION', 'PROGRESS', 'IMBALANCE',
                                   'NEGLECT', 'DROPOFF', 'PR')),
    exercise_id  BIGINT REFERENCES exercise (id),
    muscle       TEXT,
    severity     TEXT NOT NULL DEFAULT 'INFO' CHECK (severity IN ('INFO', 'WARN', 'HIGH')),
    title        TEXT NOT NULL,
    detail       TEXT,
    window_start DATE,
    window_end   DATE,
    metric_json  JSONB,                                     -- the numbers behind the insight
    detected_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    status       TEXT NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'DISMISSED', 'RESOLVED')),
    -- Re-runs upsert instead of duplicating. NULLS NOT DISTINCT (PG15+) so a NULL exercise_id
    -- or muscle still collides on the natural key.
    CONSTRAINT uq_insight UNIQUE NULLS NOT DISTINCT (type, exercise_id, muscle, window_end)
);

-- ============================================================
-- Indexes for the hot paths (CLAUDE.md §3)
-- ============================================================
CREATE INDEX idx_exercise_set_exercise_workout ON exercise_set (exercise_id, workout_id);
CREATE INDEX idx_workout_started_at            ON workout (started_at);
CREATE INDEX idx_insight_status_detected_at    ON insight (status, detected_at);
-- exercise_weekly_stat(exercise_id, iso_year, iso_week) is already covered by its primary key.
