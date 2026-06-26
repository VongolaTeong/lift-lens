import { defineStore } from 'pinia'
import type { MuscleVolumePoint } from '~/types/api'

export const useMusclesStore = defineStore('muscles', () => {
  const points = ref<MuscleVolumePoint[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const weeks = ref(12)

  async function load(w = weeks.value, force = false) {
    if (points.value.length && w === weeks.value && !force) return
    weeks.value = w
    loading.value = true
    error.value = null
    try {
      points.value = await useApi().getMuscleVolume(w)
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }

  return { points, loading, error, weeks, load }
})
