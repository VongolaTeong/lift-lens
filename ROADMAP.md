# ROADMAP — LiftLens

Detailed, working breakdown of the phases in [CLAUDE.md §9](CLAUDE.md). This is the build
tracker: each phase has a **goal**, concrete **steps** (checkboxes), **done-when** acceptance
criteria, and the **tests** that prove it. Keep PRs scoped to one phase/feature (Conventional
Commits). Check boxes as you land them.

**Legend:** `[ ]` todo · `[~]` in progress · `[x]` done.
**Grounding facts** (from the real `workout_data.csv`, locked 2026-06-23): 6,345 sets · 324
workouts · 40 exercises · Dec 2024 → Jun 2026. Metric account; `set_type` always `normal`;
`superset_id`/`distance_km`/`duration_seconds`/`rpe` all empty; 17% of sets are bodyweight
(empty `weight_kg`), including Pull Up — the named REGRESSION example.

---

## Status at a glance

| Phase | Name | State |
|------:|------|-------|
| 0 | Foundation | [x] ✅ build green, 40 exercises seeded, Testcontainers passing |
| 1 | Ingestion (ETL) | [ ] |
| 2 | Core analytics | [ ] |
| 3 | Insight engine + jobs | [ ] |
| 4 | API | [ ] |
| 5 | Frontend | [ ] |
| 6 | Ship | [ ] |
| 7 | Hevy API live sync (optional) | [ ] |

---

## Phase 0 — Foundation

**Goal:** a buildable multi-module skeleton, a real DB with a baseline migration, a green
Testcontainers harness, and the exercise→muscle seed mapping. No business logic yet.

### Steps
- [x] Gradle (Kotlin DSL) multi-module: root + `:api` (Spring Boot app) + `:analytics`
      (pure Java library, **no Spring/DB deps**).
- [x] Pin Java LTS toolchain (21, foojay auto-provision) and Spring Boot 3.4.1 BOM;
      wire `:api` → depends on `:analytics`.
- [x] Add dependencies: `spring-boot-starter-web`, `-data-jpa`, `-security`,
      `-validation`, Flyway (+`flyway-database-postgresql`), PostgreSQL driver, springdoc-openapi;
      test: JUnit 5, Testcontainers (`postgresql`, `junit-jupiter`), AssertJ.
- [x] `application.yml`: `spring.jpa.hibernate.ddl-auto=validate` (never `update`), Flyway on,
      datasource via env vars.
- [x] `docker-compose.yml` with Postgres 16 for local dev (`liftlens`/`liftlens` on `:5432`).
- [x] Flyway baseline migration `V1__baseline.sql`: all tables from [CLAUDE.md §3]
      (`exercise`, `import_batch`, `workout`, `exercise_set`, `exercise_daily_stat`,
      `exercise_weekly_stat`, `muscle_weekly_volume`, `insight`) + the hot-path indexes.
      Includes `exercise_set.load_basis`, nullable `weight_kg`, and the `insight`
      `UNIQUE NULLS NOT DISTINCT` upsert key.
- [x] Testcontainers test (`@SpringBootTest` + `@Testcontainers` + `@ServiceConnection`) that boots
      Postgres 16, runs Flyway, and asserts schema + seed + `ddl-auto=validate` all agree.
- [x] **Lock the CSV schema** — verified facts live in [CLAUDE.md §2].
- [x] Curate the **exercise→muscle seed mapping** for the 40 real exercises via
      `V2__seed_exercises.sql`.
- [x] CI (GitHub Actions): `.github/workflows/ci.yml` builds + tests on push/PR (Docker on runner).

### Seed mapping notes (the 40 exercises in this export)
Map each `hevy_name` → `canonical_name`, `primary_muscle`, `secondary_muscles`, `equipment`,
`movement_type` (COMPOUND/ISOLATION), `is_unilateral`. Watch these:
- **Bodyweight set** (empty weight): Pull Up, Chin Up, Scapular Pull Ups, Triceps Dip,
  Hanging Leg Raise, Leg Raise Parallel Bars → `load_basis=BODYWEIGHT`.
- **Mixed**: `Triceps Dip (Weighted)` (205/247 unweighted), `Overhead Press (Barbell)` — decide
  per set, not per exercise.
- Unknown Hevy names (future exports) → `primary_muscle = UNKNOWN`, surfaced in the unmapped list.

### Done when ✅
- [x] `./gradlew build` is green; Testcontainers boots Postgres and Flyway validates.
- [x] `exercise` table is seeded with all 40 exercises mapped (no `UNKNOWN` for the known set).

---

## Phase 1 — Ingestion (ETL)

**Goal:** `POST /api/imports` ingests the real CSV idempotently. Re-importing the same or an
overlapping export does **not** double-count.

### Steps
- [ ] **CSV reader** (e.g. Apache Commons CSV / Jackson CSV) tolerant of quoted fields,
      embedded commas (`Seated Cable Row - Bar Wide Grip`), and empty trailing columns.
- [ ] **Unit detection** from the header: `weight_kg`/`distance_km` vs `weight_lbs`/`distance_miles`;
      normalize to **kg + metres** at the ingestion boundary only.
- [ ] **Date parsing**: `d MMM yyyy, HH:mm` with `Locale.ENGLISH` (non-padded day, e.g. `1 Feb 2026`).
- [ ] **Checksum**: sha256 the raw file → `import_batch.checksum`; if it exists, short-circuit
      with "already imported" (HTTP 200 + existing batch summary).
- [ ] **Group** rows into workouts by (`title`,`start_time`); compute `hevy_natural_key`
      (hash of title+start_time).
- [ ] **Upsert workouts** on `hevy_natural_key` (idempotent).
- [ ] **Resolve exercises**: `exercise_title` → `exercise`; create a stub flagged for mapping if unknown.
- [ ] **Insert sets**: map columns; keep `weight_kg` NULL for bodyweight (never coerce to 0);
      derive `is_working` (not warmup) and `load_basis` (`WEIGHTED` if `weight_kg` present else `BODYWEIGHT`).
- [ ] **Set-level dedupe** within a workout on (`workout`,`exercise`,`set_index`) so overlapping
      re-exports update rather than duplicate.
- [ ] **Classify `split_category`** by majority vote of the workout's exercises' primary muscles
      (push→PUSH, pull→PULL, mixed upper→UPPER, legs→LOWER, ties/unknown→OTHER). Remember
      `Push pull` titles are genuinely mixed — never trust the title.
- [ ] **Enqueue incremental recompute** for the affected date range only (hand off to Phase 2 job).
- [ ] Return a **batch summary**: workouts added/updated, sets inserted, unknown exercises needing mapping.

### Tests
- [ ] Parse the real `workout_data.csv` → exactly **324 workouts, 6,345 sets, 40 exercises**.
- [ ] **Idempotency**: import the same file twice → 2nd import is a no-op (checksum short-circuit),
      counts unchanged.
- [ ] **Overlap idempotency**: import file A, then a superset file A′ that re-includes A's rows →
      no duplicate workouts/sets; only new rows added.
- [ ] Bodyweight rows land with `weight_kg IS NULL` and `load_basis = BODYWEIGHT`.
- [ ] Unit detection: a synthetic lbs-header file normalizes correctly to kg.
- [ ] Date edge: non-padded day parses; `lbs`/`miles` header variant handled.

### Done when
- Re-import is provably a no-op (test) and the real file produces the expected counts.

---

## Phase 2 — Core analytics

**Goal:** pure, unit-tested math in `:analytics`; materialized daily/weekly/muscle stats via
SQL window functions; PR detection. The bodyweight path must trend Pull Up.

### Steps (pure math — `:analytics`, no DB)
- [ ] **e1RM**: Epley `w×(1+reps/30)` (default) and Brzycki `w×36/(37−reps)`; exclude/flag reps > ~12.
- [ ] **Working volume**: `Σ(weight×reps)` over working sets only.
- [ ] **Bodyweight progression** (NULL weight): use **reps as the signal** (best-set reps,
      total reps over window); detectors read this series, not e1RM. Driven by `load_basis`.
- [ ] **Trend slope**: least-squares linear regression over a trailing window (default 8 weeks)
      for both the e1RM series (weighted) and the reps series (bodyweight).
- [ ] All of the above unit-tested in isolation (no Spring, no DB).

### Steps (materialization — `:api`, SQL/Testcontainers)
- [ ] `exercise_daily_stat`: per (`exercise_id`,`stat_date`) → `top_working_e1rm`, `working_volume`,
      `working_sets`, `max_weight`, `total_reps`.
- [ ] `exercise_weekly_stat`: ISO year/week → `best_e1rm`, `volume`, `sets`, `sessions`,
      `e1rm_slope` (trailing-window regression).
- [ ] `muscle_weekly_volume`: per muscle/week → `working_volume`, `set_count`, `session_count`.
- [ ] Compute moving averages / deltas in **SQL** with `LAG()` and `AVG() OVER (... ROWS BETWEEN ...)`
      (deliberately in Postgres — it's part of the signal).
- [ ] **Incremental + idempotent** recompute scoped to a date range (re-running yields identical rows).
- [ ] **PR detection**: flag `exercise_set.is_pr` for new max weight / max e1RM / max volume.

### Tests
- [ ] e1RM/volume/slope golden-value unit tests.
- [ ] Bodyweight: Pull Up produces a non-empty reps-based trend (the REGRESSION example must have data).
- [ ] Window-function queries verified against seeded fixtures (Testcontainers).
- [ ] Recompute idempotency: run twice → identical materialized rows.

### Done when
- Materialized tables populate from the real data; Pull Up has a trendable series; PRs flagged.

---

## Phase 3 — Insight engine + scheduled jobs

**Goal:** independent detector strategies upserting/resolving insights, plus the three idempotent
jobs with the free-tier external-cron fallback.

### Steps (detectors — `InsightDetector` strategy, open/closed)
- [ ] Define `InsightDetector` interface; each detector reads materialized stats, writes insights.
- [ ] `PLATEAU` — `|e1rm_slope|` below ε over window AND ≥ N data points.
- [ ] `REGRESSION` — slope significantly negative (validate against the Pull Up reps series).
- [ ] `PROGRESS` — slope strongly positive.
- [ ] `IMBALANCE` — antagonist/related weekly-volume ratio outside target band.
- [ ] `NEGLECT` — a primary muscle below min weekly sets for ≥ N weeks.
- [ ] `DROPOFF` — an exercise once regular, now lapsed > X sessions.
- [ ] `PR` — surfaced from Phase 2 PR flags.
- [ ] Thresholds in config (not hardcoded).
- [ ] **Upsert** on (`type`,`exercise_id`,`muscle`,`window_end`); **resolve** insights whose
      condition no longer holds (status → RESOLVED).

### Steps (jobs — all idempotent)
- [ ] `recomputeStatsJob` — nightly + on-import (affected range only).
- [ ] `detectInsightsJob` — weekly (e.g. Sun 22:00): run all detectors, upsert + resolve.
- [ ] `prScanJob` — on-import: flag `is_pr`, raise `PR` insights.
- [ ] `@Scheduled` for local/always-on **and** token-protected `POST /internal/jobs/{jobName}`.
- [ ] External cron (GitHub Actions / cron-job.org) calling the endpoint with a secret header;
      document both paths.

### Tests
- [ ] Each detector: positive + negative fixture (fires when it should, silent otherwise).
- [ ] Resolve lifecycle: condition clears → insight flips to RESOLVED on next run.
- [ ] Re-running a job (both `@Scheduled` and `/internal` paths) does not duplicate insights.
- [ ] Adding a new detector requires **no change** to existing ones.

### Done when
- All detectors run on real data, produce sane insights (incl. the Pull Up regression),
  and re-runs are idempotent via both invocation paths.

---

## Phase 4 — API

**Goal:** the REST surface from [CLAUDE.md §7], DTOs as records, OpenAPI docs, token auth.

### Steps
- [ ] DTOs as Java **records**; never expose entities.
- [ ] `POST /api/imports` (multipart) → batch summary.
- [ ] `GET /api/dashboard/summary` — this week vs last: volume by muscle, active insights, recent PRs.
- [ ] `GET /api/exercises` (with mapping status) · `GET /api/exercises/unmapped` ·
      `PUT /api/exercises/{id}/mapping`.
- [ ] `GET /api/exercises/{id}/trend?weeks=` — e1RM + volume (or reps for bodyweight) series.
- [ ] `GET /api/workouts?from=&to=` · `GET /api/muscles/volume?weeks=`.
- [ ] `GET /api/insights?status=active` · `POST /api/insights/{id}/dismiss`.
- [ ] `POST /internal/jobs/{jobName}` (token-protected).
- [ ] **Auth**: Spring Security single static API token (one principal); protect writes + `/internal/*`.
- [ ] springdoc-openapi → Swagger UI; annotate endpoints/DTOs.

### Tests
- [ ] `@WebMvcTest`/slice tests per endpoint (happy path + auth required on writes/internal).
- [ ] Trend endpoint returns the reps series for a bodyweight exercise.
- [ ] OpenAPI spec generates and is served.

### Done when
- All endpoints work end-to-end against seeded data; Swagger UI lists them; writes require the token.

---

## Phase 5 — Frontend (Nuxt 3)

**Goal:** the polished demo surface — the resume screenshot.

### Steps
- [ ] Nuxt 3 + TS scaffold; Pinia for state; API client **typed from the OpenAPI schema**.
- [ ] **Dashboard**: this-week volume per muscle vs last week, active-insight cards (color by
      severity), recent PRs.
- [ ] **Exercise detail**: e1RM/volume trend (or reps trend for bodyweight) via vue-echarts,
      PR history, raw set table with filters.
- [ ] **Muscle balance**: weekly volume per muscle, target vs actual, neglect highlights.
- [ ] **Insights feed**: list + dismiss; deep-link to the relevant exercise.
- [ ] **Import**: drag-drop CSV → batch result + unmapped-exercise prompts.
- [ ] Handle the bodyweight case in charts (label "reps progression" when no e1RM).
- [ ] Loading/empty/error states; responsive layout.

### Done when
- All five views render against the live API with the real data and look portfolio-grade.

---

## Phase 6 — Ship

**Goal:** deployed, seeded with real ~18 months of data, with a README that pays off the resume.

### Steps
- [ ] Provision **Neon** (Postgres); run Flyway on deploy.
- [ ] Deploy `:api` to **Koyeb/Railway**; configure env (DB URL, API token, job secret).
- [ ] Deploy Nuxt to **Cloudflare Pages**; point at the API.
- [ ] Wire the **external cron** (GitHub Actions / cron-job.org) at the deployed `/internal/jobs/*`.
- [ ] Seed production with the real export; verify counts + insights.
- [ ] **README**: one-paragraph what+why with **live demo link**; architecture diagram
      (ingest → normalize → materialize → detect → API → UI); "key decisions" (scheduled
      materialization + window functions, idempotent ingestion, detector-strategy pattern,
      free-tier scheduling); local-run (Docker Compose) + how to load your own Hevy export.

### Done when
- Live demo URL works on seeded real data; README is complete with diagram + decisions.

---

## Phase 7 — Hevy API live sync (optional)

**Goal:** live sync behind the existing normalization layer — only if time allows.

### Steps
- [ ] **Verify the actual Hevy API shape** before building (do not assume).
- [ ] Adapter that maps API payloads → the same normalized workout/set model as CSV.
- [ ] Reuse dedupe/idempotency on `hevy_natural_key`; schedule incremental pulls.
- [ ] Toggle source (`CSV`/`API`) in `import_batch.source`.

### Done when
- A scheduled API pull produces the same normalized data as a CSV import, idempotently.

---

## Cross-cutting (applies to every phase)
- Constructor injection only; records for DTOs/value objects.
- Flyway for all schema changes; `ddl-auto=validate`, never `update`.
- Normalize units at the ingestion boundary, never deeper; `weight_kg` as `NUMERIC`, nullable.
- `:analytics` has **zero** framework/DB deps and is unit-tested in isolation.
- SQL/window-function queries covered by Testcontainers integration tests.
- Conventional Commits; one phase/feature per PR.
