<script setup lang="ts">
import type { ExerciseSummary, MovementType } from '~/types/api'

const props = defineProps<{ exercise: ExerciseSummary; knownMuscles: string[] }>()
const emit = defineEmits<{ close: []; saved: [] }>()

const store = useExercisesStore()

const primaryMuscle = ref(props.exercise.mapped ? props.exercise.primaryMuscle : '')
const movementType = ref<MovementType>(
  props.exercise.movementType === 'UNKNOWN' ? 'COMPOUND' : props.exercise.movementType,
)
const equipment = ref(props.exercise.equipment ?? '')
const secondary = ref(props.exercise.secondaryMuscles.join(', '))
const unilateral = ref(props.exercise.unilateral)

const saving = ref(false)
const error = ref<string | null>(null)

const canSave = computed(() => primaryMuscle.value.trim().length > 0)

async function save() {
  if (!canSave.value) return
  saving.value = true
  error.value = null
  try {
    await store.updateMapping(props.exercise.id, {
      primaryMuscle: primaryMuscle.value.trim().toUpperCase(),
      movementType: movementType.value,
      equipment: equipment.value.trim() || null,
      secondaryMuscles: secondary.value
        .split(',')
        .map((s) => s.trim().toUpperCase())
        .filter(Boolean),
      unilateral: unilateral.value,
    })
    emit('saved')
    emit('close')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="dialog card card-pad-lg" role="dialog" aria-modal="true">
      <div class="row-between" style="margin-bottom: 14px">
        <h3 style="font-size: 17px">Map “{{ exercise.hevyName }}”</h3>
        <button class="btn-ghost btn-sm" @click="emit('close')">✕</button>
      </div>

      <div class="stack">
        <div class="field">
          <label>Primary muscle *</label>
          <input v-model="primaryMuscle" list="muscle-options" placeholder="e.g. CHEST" />
          <datalist id="muscle-options">
            <option v-for="m in knownMuscles" :key="m" :value="m" />
          </datalist>
        </div>

        <div class="field">
          <label>Movement type</label>
          <select v-model="movementType">
            <option value="COMPOUND">Compound</option>
            <option value="ISOLATION">Isolation</option>
            <option value="UNKNOWN">Unknown</option>
          </select>
        </div>

        <div class="field">
          <label>Secondary muscles (comma-separated)</label>
          <input v-model="secondary" placeholder="e.g. TRICEPS, SHOULDERS" />
        </div>

        <div class="field">
          <label>Equipment</label>
          <input v-model="equipment" placeholder="e.g. BARBELL" />
        </div>

        <label class="check">
          <input type="checkbox" v-model="unilateral" />
          <span>Unilateral (single-limb)</span>
        </label>

        <p v-if="error" class="text-bad" style="font-size: 13px; margin: 0">{{ error }}</p>

        <div class="row" style="justify-content: flex-end; gap: 10px; margin-top: 4px">
          <button class="btn btn-sm" @click="emit('close')">Cancel</button>
          <button class="btn btn-primary btn-sm" :disabled="!canSave || saving" @click="save">
            {{ saving ? 'Saving…' : 'Save mapping' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(4, 6, 10, 0.6);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.dialog {
  width: 100%;
  max-width: 440px;
  box-shadow: var(--shadow);
}

.check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-muted);
  cursor: pointer;
}

.check input {
  width: auto;
}
</style>
