import { defineStore } from 'pinia'
import type { WorkoutSummary } from '~/types/api'

export const useWorkoutsStore = defineStore('workouts', () => {
  const items = ref<WorkoutSummary[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const from = ref<string>('')
  const to = ref<string>('')

  async function load(force = false) {
    if (items.value.length && !force) return
    loading.value = true
    error.value = null
    try {
      items.value = await useApi().getWorkouts(from.value, to.value)
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }

  async function setRange(newFrom: string, newTo: string) {
    from.value = newFrom
    to.value = newTo
    await load(true)
  }

  return { items, loading, error, from, to, load, setRange }
})
