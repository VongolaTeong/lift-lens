import { defineStore } from 'pinia'
import type { ExerciseMappingRequest, ExerciseSummary, ExerciseTrend } from '~/types/api'

export const useExercisesStore = defineStore('exercises', () => {
  const items = ref<ExerciseSummary[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Cached trend series keyed by `${id}:${weeks}`.
  const trends = ref<Record<string, ExerciseTrend>>({})

  const unmapped = computed(() => items.value.filter((e) => !e.mapped))

  function byId(id: number): ExerciseSummary | undefined {
    return items.value.find((e) => e.id === id)
  }

  async function load(force = false) {
    if (items.value.length && !force) return
    loading.value = true
    error.value = null
    try {
      items.value = await useApi().getExercises()
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }

  async function loadTrend(id: number, weeks = 12, force = false): Promise<ExerciseTrend> {
    const key = `${id}:${weeks}`
    if (trends.value[key] && !force) return trends.value[key]
    const trend = await useApi().getExerciseTrend(id, weeks)
    trends.value[key] = trend
    return trend
  }

  async function updateMapping(id: number, body: ExerciseMappingRequest): Promise<ExerciseSummary> {
    const updated = await useApi().updateMapping(id, body)
    const idx = items.value.findIndex((e) => e.id === id)
    if (idx >= 0) items.value[idx] = { ...items.value[idx], ...updated }
    return updated
  }

  return { items, loading, error, trends, unmapped, byId, load, loadTrend, updateMapping }
})
