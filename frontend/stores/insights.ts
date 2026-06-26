import { defineStore } from 'pinia'
import type { Insight, InsightStatusFilter } from '~/types/api'

export const useInsightsStore = defineStore('insights', () => {
  const items = ref<Insight[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const filter = ref<InsightStatusFilter>('active')

  async function load(status: InsightStatusFilter = filter.value, force = false) {
    if (items.value.length && status === filter.value && !force) return
    filter.value = status
    loading.value = true
    error.value = null
    try {
      items.value = await useApi().getInsights(status)
    } catch (e) {
      error.value = errorMessage(e)
    } finally {
      loading.value = false
    }
  }

  async function dismiss(id: number) {
    await useApi().dismissInsight(id)
    // Drop it from the current view unless we're showing dismissed/all.
    if (filter.value === 'active' || filter.value === 'resolved') {
      items.value = items.value.filter((i) => i.id !== id)
    } else {
      const found = items.value.find((i) => i.id === id)
      if (found) found.status = 'DISMISSED'
    }
  }

  return { items, loading, error, filter, load, dismiss }
})
