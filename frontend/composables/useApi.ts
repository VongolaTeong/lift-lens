import type {
  DashboardSummary,
  ExerciseMappingRequest,
  ExerciseSummary,
  ExerciseTrend,
  ImportSummary,
  Insight,
  InsightStatusFilter,
  MuscleVolumePoint,
  WorkoutSummary,
} from '~/types/api'

/**
 * Typed thin client over the LiftLens REST API. Reads are public; writes send the static API token
 * in the X-API-Token header (api §7). Base URL + token come from runtime config so the same build
 * points at localhost in dev and the deployed API in production.
 */
export function useApi() {
  const {
    public: { apiBase, apiToken },
  } = useRuntimeConfig()

  const writeHeaders = { 'X-API-Token': apiToken as string }
  const baseURL = apiBase as string

  return {
    getDashboard: () => $fetch<DashboardSummary>('/api/dashboard/summary', { baseURL }),

    getExercises: () => $fetch<ExerciseSummary[]>('/api/exercises', { baseURL }),

    getUnmappedExercises: () => $fetch<ExerciseSummary[]>('/api/exercises/unmapped', { baseURL }),

    getExerciseTrend: (id: number, weeks = 12) =>
      $fetch<ExerciseTrend>(`/api/exercises/${id}/trend`, { baseURL, query: { weeks } }),

    updateMapping: (id: number, body: ExerciseMappingRequest) =>
      $fetch<ExerciseSummary>(`/api/exercises/${id}/mapping`, {
        baseURL,
        method: 'PUT',
        headers: writeHeaders,
        body,
      }),

    getWorkouts: (from?: string, to?: string) =>
      $fetch<WorkoutSummary[]>('/api/workouts', {
        baseURL,
        query: { from: from || undefined, to: to || undefined },
      }),

    getMuscleVolume: (weeks = 12) =>
      $fetch<MuscleVolumePoint[]>('/api/muscles/volume', { baseURL, query: { weeks } }),

    getInsights: (status: InsightStatusFilter = 'active') =>
      $fetch<Insight[]>('/api/insights', { baseURL, query: { status } }),

    dismissInsight: (id: number) =>
      $fetch<void>(`/api/insights/${id}/dismiss`, {
        baseURL,
        method: 'POST',
        headers: writeHeaders,
      }),

    importCsv: (file: File) => {
      const form = new FormData()
      form.append('file', file)
      return $fetch<ImportSummary>('/api/imports', {
        baseURL,
        method: 'POST',
        headers: writeHeaders,
        body: form,
      })
    },
  }
}
