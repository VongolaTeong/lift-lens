/**
 * API contract types — the shapes of the LiftLens REST DTOs (api §7).
 *
 * These mirror the OpenAPI schema served at /v3/api-docs. Run `npm run gen:api` against a running
 * backend to regenerate the full machine schema into types/openapi.d.ts; these hand-curated aliases
 * are what the app/stores consume so component code stays readable.
 */

export type InsightType =
  | 'PLATEAU'
  | 'REGRESSION'
  | 'PROGRESS'
  | 'IMBALANCE'
  | 'NEGLECT'
  | 'DROPOFF'
  | 'PR'

export type Severity = 'INFO' | 'WARN' | 'HIGH'

export type InsightStatus = 'ACTIVE' | 'DISMISSED' | 'RESOLVED'

/** Lowercase filter values accepted by GET /api/insights?status=. */
export type InsightStatusFilter = 'active' | 'dismissed' | 'resolved' | 'all'

export type LoadBasis = 'WEIGHTED' | 'BODYWEIGHT'

export type MovementType = 'COMPOUND' | 'ISOLATION' | 'UNKNOWN'

export type SplitCategory = 'PUSH' | 'PULL' | 'UPPER' | 'LOWER' | 'OTHER'

export interface ExerciseSummary {
  id: number
  hevyName: string
  canonicalName: string
  primaryMuscle: string
  secondaryMuscles: string[]
  equipment: string | null
  movementType: MovementType
  unilateral: boolean
  mapped: boolean
  workingSets: number
  lastTrained: string | null
}

export interface TrendPoint {
  weekStart: string
  isoYear: number
  isoWeek: number
  bestE1rm: number | null
  volume: number | null
  bestReps: number | null
  totalReps: number | null
  sets: number
  sessions: number
  e1rmSlope: number | null
  repsSlope: number | null
}

export interface ExerciseTrend {
  exerciseId: number
  exerciseName: string
  weighted: boolean
  points: TrendPoint[]
}

export interface ExerciseMappingRequest {
  primaryMuscle: string
  secondaryMuscles?: string[]
  equipment?: string | null
  movementType: MovementType
  unilateral?: boolean
}

export interface WorkoutSummary {
  id: number
  title: string
  startedAt: string
  endedAt: string | null
  durationSeconds: number | null
  splitCategory: SplitCategory
  exerciseCount: number
  setCount: number
  workingVolume: number
}

export interface MuscleVolumePoint {
  muscle: string
  weekStart: string
  isoYear: number
  isoWeek: number
  workingVolume: number
  setCount: number
  sessionCount: number
}

export interface MuscleVolumeComparison {
  muscle: string
  thisWeekVolume: number
  lastWeekVolume: number
  thisWeekSets: number
  lastWeekSets: number
}

export interface Insight {
  id: number
  type: InsightType
  severity: Severity
  exerciseId: number | null
  exerciseName: string | null
  muscle: string | null
  title: string
  detail: string | null
  windowStart: string | null
  windowEnd: string | null
  metric: Record<string, unknown> | null
  detectedAt: string
  status: InsightStatus
}

export interface Pr {
  exerciseId: number
  exerciseName: string
  date: string
  loadBasis: LoadBasis
  weightKg: number | null
  reps: number | null
  e1rm: number | null
  volume: number | null
}

export interface DashboardSummary {
  weekStart: string
  previousWeekStart: string
  volumeByMuscle: MuscleVolumeComparison[]
  activeInsights: Insight[]
  recentPrs: Pr[]
}

export interface ImportSummary {
  batchId: number | null
  status: string
  rowsParsed: number
  workoutsAdded: number
  workoutsMatched: number
  setsAdded: number
  unknownExercises: string[]
}
