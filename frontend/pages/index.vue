<script setup lang="ts">
const store = useDashboardStore()

onMounted(() => store.load())

const summary = computed(() => store.data)

const thisWeekVolume = computed(() =>
  summary.value?.volumeByMuscle.reduce((s, m) => s + m.thisWeekVolume, 0) ?? 0,
)
const lastWeekVolume = computed(() =>
  summary.value?.volumeByMuscle.reduce((s, m) => s + m.lastWeekVolume, 0) ?? 0,
)
const thisWeekSets = computed(() =>
  summary.value?.volumeByMuscle.reduce((s, m) => s + m.thisWeekSets, 0) ?? 0,
)
const volumeDelta = computed(() => pctChange(thisWeekVolume.value, lastWeekVolume.value))

const isEmpty = computed(
  () =>
    !!summary.value &&
    summary.value.volumeByMuscle.length === 0 &&
    summary.value.activeInsights.length === 0 &&
    summary.value.recentPrs.length === 0,
)
</script>

<template>
  <div class="container">
    <div class="page-header">
      <div>
        <h1>Dashboard</h1>
        <p v-if="summary">
          Week of {{ fmtDate(summary.weekStart) }} · vs week of {{ fmtDate(summary.previousWeekStart) }}
        </p>
      </div>
      <NuxtLink to="/import" class="btn btn-sm">Import data</NuxtLink>
    </div>

    <StateWrapper
      :loading="store.loading && !summary"
      :error="store.error"
      :empty="isEmpty"
      empty-text="No training data yet"
      empty-hint="Import a Hevy CSV export to populate your dashboard."
      min-height="320px"
      @retry="store.load(true)"
    >
      <template v-if="summary">
        <section class="grid grid-4" style="margin-bottom: 18px">
          <StatCard
            label="Volume this week"
            :value="fmtKg(thisWeekVolume, 0)"
            :sub="volumeDelta === null ? 'no prior week' : `${fmtSigned(volumeDelta, 0)}% vs last week`"
            :tone="volumeDelta !== null && volumeDelta >= 0 ? 'good' : 'bad'"
          />
          <StatCard label="Working sets" :value="fmtNumber(thisWeekSets)" sub="this week" />
          <StatCard
            label="Active insights"
            :value="summary.activeInsights.length"
            :sub="summary.activeInsights.length ? 'needs attention' : 'all clear'"
            :tone="summary.activeInsights.length ? 'warn' : 'good'"
          />
          <StatCard
            label="Recent PRs"
            :value="summary.recentPrs.length"
            sub="last 4 weeks"
            tone="good"
          />
        </section>

        <section class="grid grid-2" style="margin-bottom: 18px; align-items: start">
          <div class="card card-pad-lg">
            <div class="card-title">Volume by muscle — this week vs last</div>
            <WeekCompareChart
              v-if="summary.volumeByMuscle.length"
              :comparisons="summary.volumeByMuscle"
            />
            <p v-else class="muted">No volume recorded in the last two weeks.</p>
          </div>

          <div class="card card-pad-lg">
            <div class="card-title">Recent personal records</div>
            <StateWrapper :empty="summary.recentPrs.length === 0" empty-text="No PRs in the last 4 weeks" min-height="120px">
              <ul class="pr-list">
                <li v-for="(pr, i) in summary.recentPrs" :key="i">
                  <NuxtLink :to="`/exercises/${pr.exerciseId}`" class="pr-name">{{ pr.exerciseName }}</NuxtLink>
                  <span class="pr-detail mono">
                    {{ pr.loadBasis === 'BODYWEIGHT' ? `${pr.reps} reps` : fmtKg(pr.weightKg) }}
                    <template v-if="pr.e1rm"> · e1RM {{ fmtKg(pr.e1rm) }}</template>
                  </span>
                  <span class="spacer" />
                  <span class="dim">{{ fmtDate(pr.date) }}</span>
                </li>
              </ul>
            </StateWrapper>
          </div>
        </section>

        <section>
          <div class="row-between" style="margin-bottom: 12px">
            <h2 style="font-size: 18px">Active insights</h2>
            <NuxtLink to="/insights" class="btn-ghost btn-sm">View all →</NuxtLink>
          </div>
          <StateWrapper
            :empty="summary.activeInsights.length === 0"
            empty-text="No active insights"
            empty-hint="Nothing flagged right now — keep it up."
            min-height="120px"
          >
            <div class="grid grid-3">
              <InsightCard
                v-for="ins in summary.activeInsights"
                :key="ins.id"
                :insight="ins"
              />
            </div>
          </StateWrapper>
        </section>
      </template>
    </StateWrapper>
  </div>
</template>

<style scoped>
.pr-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.pr-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}

.pr-list li:last-child {
  border-bottom: none;
}

.pr-name {
  color: var(--accent);
  font-weight: 600;
}

.pr-name:hover {
  text-decoration: underline;
}

.pr-detail {
  color: var(--text-muted);
  font-size: 13px;
}
</style>
