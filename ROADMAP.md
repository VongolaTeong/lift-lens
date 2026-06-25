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
| 1 | Ingestion (ETL) | [x] ✅ real export ingests 324/6345/40, idempotent re-import verified |
| 2 | Core analytics | [x] ✅ window-fn materialization + PR detection; Pull Up reps trend verified |
| 3 | Insight engine + jobs | [x] ✅ 7 detectors + upsert/resolve + scheduled & token-gated jobs |
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
- [x] **CSV reader** (Apache Commons CSV) tolerant of quoted fields, embedded commas, and empty
      cells — [HevyCsvParser](api/src/main/java/com/liftlens/ingest/HevyCsvParser.java).
- [x] **Unit detection** from the header: `weight_kg`/`distance_km` vs `weight_lbs`/`distance_miles`;
      normalized to **kg + metres** at the ingestion boundary only.
- [x] **Date parsing**: `d MMM yyyy, HH:mm` with `Locale.ENGLISH` (non-padded day, e.g. `1 Feb 2026`).
- [x] **Checksum**: sha256 the raw file → `import_batch.checksum`; if it exists, short-circuit
      with `ALREADY_IMPORTED` + existing batch summary.
- [x] **Group** rows into workouts by (`title`,`start_time`); compute `hevy_natural_key`.
- [x] **Upsert workouts** on `hevy_natural_key` (idempotent).
- [x] **Resolve exercises**: `exercise_title` → `exercise`; create a stub flagged for mapping if unknown.
- [x] **Insert sets**: keep `weight_kg` NULL for bodyweight (never coerce to 0);
      derive `is_working` (not warmup) and `load_basis`.
- [x] **Set-level dedupe** on (`workout`,`exercise`,`set_index`) so overlapping re-exports don't duplicate.
- [x] **Classify `split_category`** by majority vote of primary muscles
      ([SplitClassifier](api/src/main/java/com/liftlens/ingest/SplitClassifier.java)) — title never trusted.
- [~] **Enqueue incremental recompute** for the affected date range — affected range is computed and
      logged as a hook; Phase 2 wires the actual recompute (`TODO(Phase 2)` in `ImportService`).
- [x] Return a **batch summary**
      ([ImportSummary](api/src/main/java/com/liftlens/ingest/ImportSummary.java)).
- [x] `POST /api/imports` controller + temporary permit-all `SecurityConfig` (Phase 4 adds the token).

### Tests ✅ (19 green, 0 skipped)
- [x] Parse the real `workout_data.csv` → exactly **324 workouts, 6,345 sets, 40 exercises**
      ([RealExportIT](api/src/test/java/com/liftlens/RealExportIT.java)).
- [x] **Idempotency**: import the same file twice → 2nd is a no-op (checksum), counts unchanged.
- [x] **Overlap idempotency**: file A then a superset of A → only new rows added.
- [x] Bodyweight rows land with `weight_kg IS NULL` and `load_basis = BODYWEIGHT`.
- [x] Unit detection: a synthetic lbs-header file normalizes to kg; km→metres.
- [x] Date edge: non-padded day parses; missing-required-column rejected.

### Done when ✅
- [x] Re-import is provably a no-op (test) and the real file produces the expected counts.

---

## Phase 2 — Core analytics

**Goal:** pure, unit-tested math in `:analytics`; materialized daily/weekly/muscle stats via
SQL window functions; PR detection. The bodyweight path must trend Pull Up.

### Steps (pure math — `:analytics`, no DB)
- [x] **e1RM**: Epley + Brzycki with a reliable-rep ceiling
      ([EstimatedOneRepMax](analytics/src/main/java/com/liftlens/analytics/EstimatedOneRepMax.java)).
- [x] **Working volume**: `Σ(weight×reps)` over working sets only (materialized in SQL).
- [x] **Bodyweight progression** (NULL weight): reps series (`best_reps`/`total_reps`/`reps_slope`),
      driven by `load_basis`; detectors read this instead of e1RM.
- [x] **Trend slope**: least-squares regression
      ([LinearRegression](analytics/src/main/java/com/liftlens/analytics/LinearRegression.java)),
      mirrored in SQL by `regr_slope` over a trailing 8-week window.
- [x] All unit-tested in isolation (no Spring, no DB) — 9 tests.

### Steps (materialization — `:api`, SQL/Testcontainers)
- [x] `exercise_daily_stat` and `exercise_weekly_stat` (+ V3 reps columns) and `muscle_weekly_volume`
      via [StatsRecomputeService](api/src/main/java/com/liftlens/stats/StatsRecomputeService.java).
- [~] Trend computed in **SQL** with window functions (`regr_slope ... OVER (... RANGE BETWEEN 7
      PRECEDING ...)`). Explicit `LAG()`/`AVG()` moving-average deltas deferred until a consumer
      (API/detectors) needs them.
- [x] **Incremental + idempotent** recompute scoped to date range (daily/muscle) + affected
      exercises (weekly, full history for correct slopes); re-run yields identical rows. Wired into
      the import hook (completes the Phase 1 TODO).
- [x] **PR detection**: [PrScanService](api/src/main/java/com/liftlens/stats/PrScanService.java) flags
      `is_pr` for new max weight / e1RM / volume (weighted) or max reps (bodyweight) via window fns.

### Tests ✅ (32 green total, 0 skipped)
- [x] e1RM/slope golden-value unit tests.
- [x] Bodyweight: Pull Up produces a non-empty reps-based trend and NO e1RM (real + crafted data).
- [x] Window-function materialization verified against fixtures (Testcontainers) — daily e1RM matches
      the analytics formula, weekly slopes positive on rising series, PRs flagged.
- [x] Recompute idempotency: run twice → identical materialized rows.

### Done when ✅
- [x] Materialized tables populate from the real data; Pull Up has a trendable series; PRs flagged.

---

## Phase 3 — Insight engine + scheduled jobs

**Goal:** independent detector strategies upserting/resolving insights, plus the three idempotent
jobs with the free-tier external-cron fallback.

### Steps (detectors — `InsightDetector` strategy, open/closed)
- [x] [InsightDetector](api/src/main/java/com/liftlens/insight/InsightDetector.java) interface;
      each reads materialized stats via [StatsReadService](api/src/main/java/com/liftlens/insight/StatsReadService.java).
- [x] `PLATEAU`, `REGRESSION` (validated on the Pull Up reps series), `PROGRESS` — slope thresholds,
      ≥ N points, stale-data guard ([detector/](api/src/main/java/com/liftlens/insight/detector/)).
- [x] `IMBALANCE` (antagonist set-count ratio — set-based so bodyweight pulling counts fairly),
      `NEGLECT` (sets/window below threshold), `DROPOFF` (lapsed lift), `PR` (from Phase 2 flags).
- [x] Thresholds in config ([InsightProperties](api/src/main/java/com/liftlens/insight/InsightProperties.java),
      `liftlens.insights.*`).
- [x] **Upsert** on the natural key (jsonb `ON CONFLICT`); **resolve** ACTIVE insights no detector
      re-emitted ([InsightDetectionService](api/src/main/java/com/liftlens/insight/InsightDetectionService.java)).

### Steps (jobs — all idempotent)
- [x] `recomputeStatsJob` — nightly @Scheduled + on-import (affected range only, Phase 2 hook).
- [x] `detectInsightsJob` — weekly @Scheduled (Sun 22:00): run all detectors, upsert + resolve.
- [x] `prScanJob` — on-import; also exposed as a triggerable job.
- [x] `@Scheduled` ([ScheduledJobs](api/src/main/java/com/liftlens/job/ScheduledJobs.java)) **and**
      token-protected `POST /internal/jobs/{jobName}` ([InternalJobController](api/src/main/java/com/liftlens/web/InternalJobController.java)
      + [InternalTokenFilter](api/src/main/java/com/liftlens/config/InternalTokenFilter.java)) →
      same [JobService](api/src/main/java/com/liftlens/job/JobService.java).
- [~] External cron wiring (GitHub Actions / cron-job.org) — endpoint + token ready; the actual cron
      job is set up at deploy (Phase 6).

### Tests ✅ (40 green total, 0 skipped)
- [x] Crafted history triggers all 7 types; idempotent re-run keeps insight count stable.
- [x] Resolve lifecycle: an ACTIVE insight no detector re-emits flips to RESOLVED.
- [x] Internal endpoint: 401 without/with wrong token, 200 with token, 404 unknown job.
- [x] Registry: every `InsightType` has exactly one registered detector (open/closed).

### Done when ✅
- [x] All detectors run (crafted + real data), produce sane insights incl. the Pull Up regression,
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
