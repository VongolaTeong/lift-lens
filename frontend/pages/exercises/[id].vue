<script setup lang="ts">
import type { ExerciseTrend } from '~/types/api'

const route = useRoute()
const store = useExercisesStore()
const id = computed(() => Number(route.params.id))

const weeks = ref(12)
const weekOptions = [8, 12, 26, 52]

const trend = ref<ExerciseTrend | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const meta = computed(() => store.byId(id.value))

async function loadTrend() {
  loading.value = true
  error.value = null
  try {
    trend.value = await store.loadTrend(id.value, weeks.value)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (!store.items.length) store.load()
  await loadTrend()
})

watch(weeks, loadTrend)

const hasPoints = computed(() => (trend.value?.points.length ?? 0) > 0)

// Peak week for PR highlighting in the table.
const peakWeek = computed(() => {
  if (!trend.value) return null
  let best = -Infinity
  let key: string | null = null
  for (const p of trend.value.points) {
    const v = trend.value.weighted ? p.bestE1rm : p.bestReps
    if (v != null && v > best) {
      best = v
      key = p.weekStart
    }
  }
  return key
})

const latestSlope = computed(() => {
  const pts = trend.value?.points ?? []
  if (!pts.length) return null
  const last = pts[pts.length - 1]
  return trend.value?.weighted ? last.e1rmSlope : last.repsSlope
})

const rowsDesc = computed(() => [...(trend.value?.points ?? [])].reverse())
</script>

<template>
  <div class="container">
    <div class="page-header">
      <div>
        <NuxtLink to="/exercises" class="btn-ghost btn-sm" style="margin-bottom: 8px; display: inline-flex">
          ← All exercises
        </NuxtLink>
        <h1>{{ trend?.exerciseName ?? meta?.canonicalName ?? 'Exercise' }}</h1>
        <p v-if="meta" class="row wrap" style="gap: 8px">
          <span v-if="meta.mapped" class="badge badge-neutral">{{ titleCase(meta.primaryMuscle) }}</span>
          <span v-else class="badge badge-warn">Unmapped</span>
          <span class="badge badge-neutral">{{ titleCase(meta.movementType) }}</span>
          <span v-if="meta.equipment" class="badge badge-neutral">{{ titleCase(meta.equipment) }}</span>
          <span
            class="badge"
            :class="trend?.weighted ? 'badge-info' : 'badge-good'"
          >
            {{ trend?.weighted ? 'Weighted (e1RM)' : 'Bodyweight (reps)' }}
          </span>
        </p>
      </div>
      <div class="field" style="min-width: 120px">
        <label>Window</label>
        <select v-model.number="weeks">
          <option v-for="w in weekOptions" :key="w" :value="w">{{ w }} weeks</option>
        </select>
      </div>
    </div>

    <StateWrapper
      :loading="loading && !trend"
      :error="error"
      :empty="!loading && !hasPoints"
      empty-text="No materialized trend yet"
      empty-hint="This exercise has no working sets in range, or stats haven’t been computed."
      min-height="340px"
      @retry="loadTrend"
    >
      <template v-if="trend && hasPoints">
        <section class="grid grid-3" style="margin-bottom: 18px">
          <StatCard
            :label="trend.weighted ? 'Latest e1RM trend' : 'Latest reps trend'"
            :value="fmtSigned(latestSlope, 2)"
            :sub="trend.weighted ? 'kg / week' : 'reps / week'"
            :tone="latestSlope != null && latestSlope > 0 ? 'good' : latestSlope != null && latestSlope < 0 ? 'bad' : 'neutral'"
          />
          <StatCard label="Weeks tracked" :value="trend.points.length" sub="in window" />
          <StatCard
            :label="trend.weighted ? 'Best e1RM' : 'Best set'"
            :value="
              trend.weighted
                ? fmtKg(Math.max(...trend.points.map((p) => p.bestE1rm ?? 0)))
                : `${Math.max(...trend.points.map((p) => p.bestReps ?? 0))} reps`
            "
            tone="good"
          />
        </section>

        <div class="card card-pad-lg" style="margin-bottom: 18px">
          <div class="card-title">
            {{ trend.weighted ? 'e1RM + volume progression' : 'Reps progression (bodyweight)' }}
          </div>
          <TrendChart :trend="trend" />
        </div>

        <div class="card-title">Weekly breakdown</div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Week</th>
                <th class="num">{{ trend.weighted ? 'Best e1RM' : 'Best reps' }}</th>
                <th class="num">{{ trend.weighted ? 'Volume' : 'Total reps' }}</th>
                <th class="num">Sets</th>
                <th class="num">Sessions</th>
                <th class="num">Slope</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in rowsDesc" :key="p.weekStart">
                <td>
                  {{ fmtDate(p.weekStart) }}
                  <span v-if="p.weekStart === peakWeek" class="badge badge-good" style="margin-left: 6px">PR</span>
                </td>
                <td class="num">{{ trend.weighted ? fmtKg(p.bestE1rm) : (p.bestReps ?? '—') }}</td>
                <td class="num">{{ trend.weighted ? fmtKg(p.volume, 0) : (p.totalReps ?? '—') }}</td>
                <td class="num">{{ p.sets }}</td>
                <td class="num">{{ p.sessions }}</td>
                <td class="num">{{ fmtSigned(trend.weighted ? p.e1rmSlope : p.repsSlope, 2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </StateWrapper>
  </div>
</template>
