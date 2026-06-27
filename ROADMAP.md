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
| 4 | API | [x] ✅ full REST surface, record DTOs, token-guarded writes, OpenAPI/Swagger |
| 5 | Frontend | [x] ✅ Nuxt 3 SPA, 5 views, vue-echarts, Pinia, typed client; live-verified vs real data |
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
- [x] DTOs as Java **records** ([web/dto/](api/src/main/java/com/liftlens/web/dto/)); entities never exposed.
- [x] `POST /api/imports` (multipart) → batch summary
      ([ImportController](api/src/main/java/com/liftlens/web/ImportController.java)); now token-guarded.
- [x] `GET /api/dashboard/summary` — this week vs last: volume by muscle, active insights, recent PRs
      ([DashboardController](api/src/main/java/com/liftlens/web/DashboardController.java) +
      [DashboardQueryService](api/src/main/java/com/liftlens/query/DashboardQueryService.java)).
- [x] `GET /api/exercises` (with mapping status) · `GET /api/exercises/unmapped` ·
      `PUT /api/exercises/{id}/mapping` ([ExerciseController](api/src/main/java/com/liftlens/web/ExerciseController.java),
      mapping via [ExerciseMappingService](api/src/main/java/com/liftlens/query/ExerciseMappingService.java) +
      `Exercise.applyMapping`).
- [x] `GET /api/exercises/{id}/trend?weeks=` — e1RM + volume (or reps for bodyweight) series; `weighted`
      flag tells the UI which series to plot.
- [x] `GET /api/workouts?from=&to=` ([WorkoutController](api/src/main/java/com/liftlens/web/WorkoutController.java)) ·
      `GET /api/muscles/volume?weeks=` ([MuscleController](api/src/main/java/com/liftlens/web/MuscleController.java)).
- [x] `GET /api/insights?status=active` · `POST /api/insights/{id}/dismiss`
      ([InsightController](api/src/main/java/com/liftlens/web/InsightController.java)).
- [x] `POST /internal/jobs/{jobName}` (token-protected) — from Phase 3, unchanged.
- [x] **Auth**: single static API token, one `ROLE_API` principal
      ([ApiTokenAuthFilter](api/src/main/java/com/liftlens/config/ApiTokenAuthFilter.java) +
      [SecurityConfig](api/src/main/java/com/liftlens/config/SecurityConfig.java)): reads public, writes on
      `/api/**` require `X-API-Token` (401 otherwise); `/internal/*` keeps its own shared-secret guard.
- [x] springdoc-openapi → Swagger UI; `@Tag`/`@Operation` on endpoints + API-token security scheme
      ([OpenApiConfig](api/src/main/java/com/liftlens/config/OpenApiConfig.java)). Errors normalized by
      [ApiExceptionHandler](api/src/main/java/com/liftlens/web/ApiExceptionHandler.java) (404/400).

### Tests ✅ (57 green total, 0 skipped)
- [x] Slice tests per endpoint (happy path) + auth required on writes (import/dismiss/mapping → 401
      without token) ([ApiEndpointsIT](api/src/test/java/com/liftlens/ApiEndpointsIT.java)).
- [x] Trend endpoint returns the reps series (no e1RM) for a bodyweight exercise; e1RM series for a
      weighted one. 404s for unknown exercise/insight; 400 for invalid mapping body / bad status.
- [x] OpenAPI spec generates and is served at `/v3/api-docs` (asserts the path entries).

### Done when ✅
- [x] All endpoints work end-to-end against seeded data; OpenAPI lists them; writes require the token.

---

## Phase 5 — Frontend (Nuxt 3)

**Goal:** the polished demo surface — the resume screenshot.

### Steps
- [x] Nuxt 3 + TS scaffold ([frontend/](frontend/), SPA `ssr:false` for Cloudflare Pages); **Pinia**
      stores per resource ([frontend/stores/](frontend/stores/)); typed `$fetch` client
      ([useApi](frontend/composables/useApi.ts)) against the API contract
      ([types/api.ts](frontend/types/api.ts), regen via `npm run gen:api` from `/v3/api-docs`).
- [x] **Dashboard** ([pages/index.vue](frontend/pages/index.vue)): this-week vs last-week volume per
      muscle (grouped bars), active-insight cards coloured by severity, recent PRs, headline stats.
- [x] **Exercise detail** ([pages/exercises/[id].vue](frontend/pages/exercises/[id].vue)): e1RM+volume
      trend (or reps trend for bodyweight) via vue-echarts with a PR marker, weekly breakdown table.
      (Per-set raw rows aren't an API endpoint; the weekly breakdown is the detail view.)
- [x] **Muscle balance** ([pages/muscles.vue](frontend/pages/muscles.vue)): weekly volume per muscle
      (multi-line, volume/sets toggle), balance bars over the window, low-volume (neglect) highlights.
- [x] **Insights feed** ([pages/insights.vue](frontend/pages/insights.vue)): status tabs + dismiss;
      cards deep-link to the relevant exercise/muscle.
- [x] **Import** ([pages/import.vue](frontend/pages/import.vue)): drag-drop CSV → batch summary +
      unmapped-exercise prompts; recent-sessions table.
- [x] Bodyweight handled in charts/tables (labelled "reps progression" when there's no e1RM).
- [x] Loading/empty/error states ([StateWrapper](frontend/components/StateWrapper.vue)); responsive grid.
- [x] CORS enabled on the API ([SecurityConfig](api/src/main/java/com/liftlens/config/SecurityConfig.java))
      so the browser SPA can call it (incl. the `X-API-Token` write header).

### Done when ✅
- [x] `npm run build` / `generate` + `vue-tsc` typecheck all green; static bundle in `.output/public`.
- [x] Live-verified against the real export (324 workouts / 6,345 sets): dashboard, exercises (40),
      insights (36 active), muscle volume (95 pts), and the Pull Up bodyweight reps trend all return
      populated data; CORS preflight from `:3000` passes.

---

## Phase 6 — Ship

**Goal:** deployed, seeded with real ~18 months of data, with a README that pays off the resume.

> **Deploy scaffolding is in place** — full step-by-step in [DEPLOYMENT.md](DEPLOYMENT.md): API
> [`Dockerfile`](Dockerfile) (builds `app.jar`, Docker build verified through the in-container Gradle
> step), `server.port=${PORT}` for Render/Railway, a Render Blueprint ([render.yaml](render.yaml)),
> Pages SPA fallback
> ([`frontend/public/_redirects`](frontend/public/_redirects)) + [`.nvmrc`](frontend/.nvmrc), and the
> external-cron workflow ([`.github/workflows/cron.yml`](.github/workflows/cron.yml)). The boxes below
> stay unchecked until the live deploy is actually run with real accounts.

### Steps
- [ ] Provision **Neon** (Postgres); run Flyway on deploy. — *steps written; Flyway runs on boot.*
- [ ] Deploy `:api` to **Render** (no card; Koyeb/Railway as alternatives); configure env (DB URL, API token, job secret). — *Dockerfile + render.yaml ready.*
- [ ] Deploy Nuxt to **Cloudflare Pages**; point at the API. — *build settings + SPA fallback ready.*
- [ ] Wire the **external cron** (GitHub Actions / cron-job.org) at the deployed `/internal/jobs/*`. — *workflow ready.*
- [ ] Seed production with the real export; verify counts + insights. — *commands documented.*
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
