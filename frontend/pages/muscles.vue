<script setup lang="ts">
const store = useMusclesStore()

const weeks = ref(12)
const weekOptions = [8, 12, 26, 52]
const metric = ref<'volume' | 'sets'>('volume')

// A muscle is flagged "low" when its working sets over the window fall below this floor.
const LOW_SET_FLOOR = 12

onMounted(() => store.load(weeks.value))
watch(weeks, (w) => store.load(w, true))

interface MuscleAgg {
  muscle: string
  totalVolume: number
  totalSets: number
  sessions: number
}

const aggregates = computed<MuscleAgg[]>(() => {
  const map = new Map<string, MuscleAgg>()
  for (const p of store.points) {
    const a =
      map.get(p.muscle) ?? { muscle: p.muscle, totalVolume: 0, totalSets: 0, sessions: 0 }
    a.totalVolume += p.workingVolume
    a.totalSets += p.setCount
    a.sessions += p.sessionCount
    map.set(p.muscle, a)
  }
  return [...map.values()].sort((a, b) => b.totalSets - a.totalSets)
})

const maxVolume = computed(() => Math.max(1, ...aggregates.value.map((a) => a.totalVolume)))
const maxSets = computed(() => Math.max(1, ...aggregates.value.map((a) => a.totalSets)))

function barWidth(a: MuscleAgg): string {
  const ratio =
    metric.value === 'volume' ? a.totalVolume / maxVolume.value : a.totalSets / maxSets.value
  return `${Math.max(2, ratio * 100)}%`
}

const lowCount = computed(() => aggregates.value.filter((a) => a.totalSets < LOW_SET_FLOOR).length)
</script>

<template>
  <div class="container">
    <div class="page-header">
      <div>
        <h1>Muscle Balance</h1>
        <p>Weekly working volume per muscle · {{ lowCount }} muscle{{ lowCount === 1 ? '' : 's' }} below {{ LOW_SET_FLOOR }} sets this window</p>
      </div>
      <div class="field" style="min-width: 120px">
        <label>Window</label>
        <select v-model.number="weeks">
          <option v-for="w in weekOptions" :key="w" :value="w">{{ w }} weeks</option>
        </select>
      </div>
    </div>

    <StateWrapper
      :loading="store.loading && !store.points.length"
      :error="store.error"
      :empty="!store.loading && store.points.length === 0"
      empty-text="No muscle volume yet"
      empty-hint="Import a Hevy export to see your muscle balance."
      min-height="340px"
      @retry="store.load(weeks, true)"
    >
      <div class="card card-pad-lg" style="margin-bottom: 18px">
        <div class="row-between" style="margin-bottom: 8px">
          <div class="card-title" style="margin: 0">Weekly volume per muscle</div>
          <div class="seg">
            <button :class="{ on: metric === 'volume' }" @click="metric = 'volume'">Volume</button>
            <button :class="{ on: metric === 'sets' }" @click="metric = 'sets'">Sets</button>
          </div>
        </div>
        <MuscleVolumeChart :points="store.points" :metric="metric" />
      </div>

      <div class="card-title">
        Balance — {{ metric === 'volume' ? 'total volume' : 'total sets' }} over {{ weeks }} weeks
      </div>
      <div class="card">
        <div class="balance">
          <div v-for="a in aggregates" :key="a.muscle" class="balance-row">
            <div class="balance-name">
              {{ titleCase(a.muscle) }}
              <span v-if="a.totalSets < LOW_SET_FLOOR" class="badge badge-warn">Low</span>
            </div>
            <div class="balance-track">
              <div
                class="balance-fill"
                :class="{ low: a.totalSets < LOW_SET_FLOOR }"
                :style="{ width: barWidth(a) }"
              />
            </div>
            <div class="balance-val mono">
              {{ metric === 'volume' ? fmtKg(a.totalVolume, 0) : `${a.totalSets} sets` }}
            </div>
          </div>
        </div>
      </div>
    </StateWrapper>
  </div>
</template>

<style scoped>
.seg {
  display: inline-flex;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.seg button {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.seg button.on {
  background: var(--accent-soft);
  color: var(--accent);
}

.balance {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.balance-row {
  display: grid;
  grid-template-columns: 140px 1fr 110px;
  align-items: center;
  gap: 14px;
}

.balance-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 550;
  font-size: 14px;
}

.balance-track {
  height: 12px;
  background: var(--bg-elevated);
  border-radius: 6px;
  overflow: hidden;
}

.balance-fill {
  height: 100%;
  border-radius: 6px;
  background: linear-gradient(90deg, var(--accent), #6f9bff);
  transition: width 0.4s ease;
}

.balance-fill.low {
  background: linear-gradient(90deg, var(--warn), #f5c66b);
}

.balance-val {
  text-align: right;
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 620px) {
  .balance-row {
    grid-template-columns: 100px 1fr 80px;
    gap: 8px;
  }
}
</style>
