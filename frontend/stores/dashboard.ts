import { defineStore } from 'pinia'
import type { DashboardSummary } from '~/types/api'

export const useDashboardStore = defineStore('dashboard', () => {
  const data = ref<DashboardSummary | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function load(force = false) {
    if (data.value && !force) return
    loading.value = true
    error.value = null
    try {
      data.value = await useApi().getDashboard()
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }

  return { data, loading, error, load }
})
