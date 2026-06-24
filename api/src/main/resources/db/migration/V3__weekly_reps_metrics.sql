-- V3 — bodyweight progression series on the weekly stat table (CLAUDE.md §5).
-- Weighted movements trend on best_e1rm/e1rm_slope; bodyweight movements (NULL weight) have no
-- e1RM, so they trend on reps instead. Pull Up — the named REGRESSION example — needs this.

ALTER TABLE exercise_weekly_stat
    ADD COLUMN best_reps  INTEGER,
    ADD COLUMN total_reps INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN reps_slope NUMERIC(10, 4);
