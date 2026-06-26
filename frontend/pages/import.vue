<script setup lang="ts">
import type { ImportSummary } from '~/types/api'

const api = useApi()
const workouts = useWorkoutsStore()
const exercises = useExercisesStore()

const dragging = ref(false)
const uploading = ref(false)
const error = ref<string | null>(null)
const result = ref<ImportSummary | null>(null)
const fileName = ref('')

const fileInput = ref<HTMLInputElement | null>(null)

onMounted(() => workouts.load())

function pick() {
  fileInput.value?.click()
}

async function handleFiles(files: FileList | null) {
  const file = files?.[0]
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.csv')) {
    error.value = 'Please choose a Hevy CSV export (.csv).'
    return
  }
  fileName.value = file.name
  uploading.value = true
  error.value = null
  result.value = null
  try {
    result.value = await api.importCsv(file)
    // Refresh data that the import just changed.
    await Promise.all([workouts.load(true), exercises.load(true)])
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    uploading.value = false
  }
}

function onDrop(e: DragEvent) {
  dragging.value = false
  handleFiles(e.dataTransfer?.files ?? null)
}

const recent = computed(() => workouts.items.slice(0, 8))
</script>

<template>
  <div class="container">
    <div class="page-header">
      <div>
        <h1>Import</h1>
        <p>Upload a Hevy CSV export. Re-importing the same or an overlapping file is idempotent.</p>
      </div>
    </div>

    <div
      class="dropzone"
      :class="{ dragging, busy: uploading }"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
      @click="pick"
    >
      <input
        ref="fileInput"
        type="file"
        accept=".csv,text/csv"
        hidden
        @change="handleFiles(($event.target as HTMLInputElement).files)"
      />
      <div v-if="uploading" class="dz-inner">
        <div class="spinner" />
        <p>Importing {{ fileName }}…</p>
      </div>
      <div v-else class="dz-inner">
        <div class="dz-glyph">⬆️</div>
        <p class="dz-title">Drop your Hevy CSV here</p>
        <p class="muted">or click to browse · exported from Hevy → Settings → Export Workouts</p>
      </div>
    </div>

    <p v-if="error" class="text-bad" style="margin-top: 14px">{{ error }}</p>

    <section v-if="result" class="card card-pad-lg" style="margin-top: 18px">
      <div class="row-between" style="margin-bottom: 14px">
        <h2 style="font-size: 18px">Import result</h2>
        <span
          class="badge"
          :class="result.status === 'ALREADY_IMPORTED' ? 'badge-neutral' : 'badge-good'"
        >
          {{ result.status }}
        </span>
      </div>

      <div class="grid grid-4">
        <StatCard label="Rows parsed" :value="fmtNumber(result.rowsParsed)" />
        <StatCard label="Workouts added" :value="fmtNumber(result.workoutsAdded)" tone="good" />
        <StatCard label="Workouts matched" :value="fmtNumber(result.workoutsMatched)" />
        <StatCard label="Sets added" :value="fmtNumber(result.setsAdded)" tone="good" />
      </div>

      <div v-if="result.unknownExercises.length" class="unmapped">
        <div class="row-between" style="margin-bottom: 8px">
          <strong>{{ result.unknownExercises.length }} new exercise(s) need a muscle mapping</strong>
          <NuxtLink to="/exercises" class="btn btn-sm">Map them →</NuxtLink>
        </div>
        <div class="row wrap">
          <span v-for="name in result.unknownExercises" :key="name" class="badge badge-warn">{{ name }}</span>
        </div>
      </div>
      <p v-else-if="result.status !== 'ALREADY_IMPORTED'" class="muted" style="margin: 14px 0 0">
        ✓ All exercises were recognised — no mapping needed.
      </p>
    </section>

    <section style="margin-top: 26px">
      <div class="row-between" style="margin-bottom: 12px">
        <h2 style="font-size: 18px">Recent sessions</h2>
        <span class="dim">{{ workouts.items.length }} total</span>
      </div>
      <StateWrapper
        :loading="workouts.loading && !workouts.items.length"
        :error="workouts.error"
        :empty="!workouts.loading && workouts.items.length === 0"
        empty-text="No sessions yet"
        empty-hint="Import a CSV to get started."
        min-height="140px"
        @retry="workouts.load(true)"
      >
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Session</th>
                <th>Date</th>
                <th>Split</th>
                <th class="num">Exercises</th>
                <th class="num">Sets</th>
                <th class="num">Volume</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="w in recent" :key="w.id">
                <td style="font-weight: 600">{{ w.title }}</td>
                <td class="muted">{{ fmtDate(w.startedAt) }}</td>
                <td><span class="badge badge-neutral">{{ titleCase(w.splitCategory) }}</span></td>
                <td class="num">{{ w.exerciseCount }}</td>
                <td class="num">{{ w.setCount }}</td>
                <td class="num">{{ fmtKg(w.workingVolume, 0) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </StateWrapper>
    </section>
  </div>
</template>

<style scoped>
.dropzone {
  border: 2px dashed var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  padding: 48px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.dropzone:hover,
.dropzone.dragging {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.dropzone.busy {
  cursor: progress;
}

.dz-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.dz-glyph {
  font-size: 34px;
}

.dz-title {
  font-size: 17px;
  font-weight: 650;
  margin: 4px 0 0;
}

.unmapped {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.spinner {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 3px solid var(--border-strong);
  border-top-color: var(--accent);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
