<script setup lang="ts">
import type { ExerciseSummary } from '~/types/api'

const store = useExercisesStore()
const router = useRouter()

onMounted(() => store.load())

const search = ref('')
const onlyUnmapped = ref(false)

const knownMuscles = computed(() =>
  [...new Set(store.items.filter((e) => e.mapped).map((e) => e.primaryMuscle))].sort(),
)

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return store.items.filter((e) => {
    if (onlyUnmapped.value && e.mapped) return false
    if (!q) return true
    return (
      e.canonicalName.toLowerCase().includes(q) ||
      e.hevyName.toLowerCase().includes(q) ||
      e.primaryMuscle.toLowerCase().includes(q)
    )
  })
})

const unmappedCount = computed(() => store.unmapped.length)

const mappingTarget = ref<ExerciseSummary | null>(null)

function openMapping(e: ExerciseSummary) {
  mappingTarget.value = e
}

function goToDetail(e: ExerciseSummary) {
  router.push(`/exercises/${e.id}`)
}
</script>

<template>
  <div class="container">
    <div class="page-header">
      <div>
        <h1>Exercises</h1>
        <p>{{ store.items.length }} exercises · {{ unmappedCount }} awaiting a muscle mapping</p>
      </div>
    </div>

    <div class="toolbar card" style="margin-bottom: 16px">
      <input v-model="search" placeholder="Search exercises or muscles…" style="flex: 1; min-width: 200px" />
      <label class="check">
        <input type="checkbox" v-model="onlyUnmapped" />
        <span>Needs mapping only</span>
        <span v-if="unmappedCount" class="badge badge-warn">{{ unmappedCount }}</span>
      </label>
    </div>

    <StateWrapper
      :loading="store.loading && !store.items.length"
      :error="store.error"
      :empty="!store.loading && filtered.length === 0"
      empty-text="No matching exercises"
      empty-hint="Try a different search, or import a Hevy export."
      min-height="320px"
      @retry="store.load(true)"
    >
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Exercise</th>
              <th>Primary muscle</th>
              <th>Type</th>
              <th>Equipment</th>
              <th class="num">Working sets</th>
              <th>Last trained</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="e in filtered"
              :key="e.id"
              class="clickable"
              @click="goToDetail(e)"
            >
              <td>
                <div class="ex-name">{{ e.canonicalName }}</div>
                <div class="dim" style="font-size: 12px">{{ e.hevyName }}</div>
              </td>
              <td>
                <span v-if="e.mapped">{{ titleCase(e.primaryMuscle) }}</span>
                <span v-else class="badge badge-warn">Unmapped</span>
              </td>
              <td class="muted">{{ titleCase(e.movementType) }}</td>
              <td class="muted">{{ titleCase(e.equipment) }}</td>
              <td class="num">{{ fmtNumber(e.workingSets) }}</td>
              <td class="muted">{{ fmtDate(e.lastTrained) }}</td>
              <td @click.stop>
                <button class="btn btn-sm" @click="openMapping(e)">
                  {{ e.mapped ? 'Edit' : 'Map' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </StateWrapper>

    <MappingDialog
      v-if="mappingTarget"
      :exercise="mappingTarget"
      :known-muscles="knownMuscles"
      @close="mappingTarget = null"
      @saved="store.load(true)"
    />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-muted);
  cursor: pointer;
  white-space: nowrap;
}

.check input {
  width: auto;
}

.ex-name {
  font-weight: 600;
}
</style>
