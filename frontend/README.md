# LiftLens — Frontend (Nuxt 3)

The demo surface for [LiftLens](../CLAUDE.md): a polished analytics dashboard over the REST API.
Built with **Nuxt 3 + TypeScript**, **Pinia** for state, and **vue-echarts** (Apache ECharts) for charts.
Deploys as a static SPA to Cloudflare Pages.

## Views

| Route             | What it shows |
|-------------------|---------------|
| `/`               | Dashboard — this-week vs last-week volume by muscle, active insight cards (coloured by severity), recent PRs. |
| `/exercises`      | Exercise list with mapping status; search + "needs mapping" filter; inline muscle-mapping dialog. |
| `/exercises/{id}` | Exercise detail — e1RM + volume trend (or **reps progression** for bodyweight lifts), weekly breakdown with PR markers. |
| `/muscles`        | Muscle balance — weekly volume per muscle, total-volume/sets balance bars, low-volume highlights. |
| `/insights`       | Insight feed with status tabs (active / resolved / dismissed / all); dismiss + deep-link to the exercise/muscle. |
| `/import`         | Drag-drop CSV import → batch summary + unmapped-exercise prompts; recent sessions table. |

The bodyweight case is handled throughout: when an exercise has no e1RM the charts and tables switch
to the reps series and label it "reps progression" (CLAUDE.md §5/§8). Every async view has explicit
loading / empty / error states.

## Setup

```bash
npm install
npm run dev          # http://localhost:3000
```

Point it at the API with runtime config (defaults shown):

```bash
NUXT_PUBLIC_API_BASE=http://localhost:8080 \
NUXT_PUBLIC_API_TOKEN=dev-api-token \
npm run dev
```

- `NUXT_PUBLIC_API_BASE` — base URL of the LiftLens API.
- `NUXT_PUBLIC_API_TOKEN` — the static API token sent as `X-API-Token` on writes (import, mapping,
  dismiss). Reads are public.

The API must allow this origin via CORS (`liftlens.cors.allowed-origins`, default `http://localhost:3000`).

## Scripts

| Script | Purpose |
|--------|---------|
| `npm run dev` | Dev server with HMR. |
| `npm run build` | Production build (`.output`). |
| `npm run generate` | Static SPA for Cloudflare Pages → deploy `.output/public`. |
| `npm run typecheck` | `vue-tsc` strict type-check. |
| `npm run gen:api` | Regenerate `types/openapi.d.ts` from the live API's `/v3/api-docs`. |

## API types

The app consumes the LiftLens REST contract through hand-curated aliases in [`types/api.ts`](types/api.ts)
that mirror the server DTOs / OpenAPI schema. Run `npm run gen:api` against a running backend to emit
the full machine schema into `types/openapi.d.ts` and diff it against the aliases when the API changes.

## Architecture

- **`composables/useApi.ts`** — typed thin client over `$fetch`; base URL + token from runtime config.
- **`stores/*`** — Pinia stores (one per resource) holding `data` / `loading` / `error`, used by pages.
- **`components/`** — chart components (`TrendChart`, `MuscleVolumeChart`, `WeekCompareChart`), shared UI
  (`StateWrapper`, `InsightCard`, `StatCard`, `MappingDialog`, `SeverityBadge`).
- **`plugins/echarts.client.ts`** — registers only the ECharts modules used (tree-shaken core build).
