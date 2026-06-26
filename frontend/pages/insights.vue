<script setup lang="ts">
import type { InsightStatusFilter } from '~/types/api'

const store = useInsightsStore()

const tabs: { value: InsightStatusFilter; label: string }[] = [
  { value: 'active', label: 'Active' },
  { value: 'resolved', label: 'Resolved' },
  { value: 'dismissed', label: 'Dismissed' },
  { value: 'all', label: 'All' },
]

onMounted(() => store.load('active'))

function setFilter(f: InsightStatusFilter) {
  store.load(f, true)
}

async function dismiss(id: number) {
  try {
    await store.dismiss(id)
  } catch (e) {
    store.error = errorMessage(e)
  }
}
</script>

<template>
  <div class="container">
    <div class="page-header">
      <div>
        <h1>Insights</h1>
        <p>Plateaus, regressions, imbalances and PRs detected from your training.</p>
      </div>
    </div>

    <div class="tabs" style="margin-bottom: 18px">
      <button
        v-for="t in tabs"
        :key="t.value"
        class="tab"
        :class="{ on: store.filter === t.value }"
        @click="setFilter(t.value)"
      >
        {{ t.label }}
      </button>
    </div>

    <StateWrapper
      :loading="store.loading && !store.items.length"
      :error="store.error"
      :empty="!store.loading && store.items.length === 0"
      :empty-text="store.filter === 'active' ? 'No active insights' : 'Nothing here'"
      empty-hint="Insights are recomputed by the weekly detector job."
      min-height="300px"
      @retry="store.load(store.filter, true)"
    >
      <div class="grid grid-3">
        <InsightCard
          v-for="ins in store.items"
          :key="ins.id"
          :insight="ins"
          :dismissible="ins.status === 'ACTIVE'"
          @dismiss="dismiss"
        />
      </div>
    </StateWrapper>
  </div>
</template>

<style scoped>
.tabs {
  display: inline-flex;
  gap: 4px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 4px;
}

.tab {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 7px 16px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 6px;
  cursor: pointer;
}

.tab.on {
  background: var(--accent-soft);
  color: var(--accent);
}
</style>
