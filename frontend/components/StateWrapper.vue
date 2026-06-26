<script setup lang="ts">
/** Wraps async UI with consistent loading / error / empty states (Phase 5 requirement). */
const props = withDefaults(
  defineProps<{
    loading?: boolean
    error?: string | null
    empty?: boolean
    emptyText?: string
    emptyHint?: string
    minHeight?: string
  }>(),
  {
    loading: false,
    error: null,
    empty: false,
    emptyText: 'Nothing here yet.',
    emptyHint: '',
    minHeight: '160px',
  },
)

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div>
    <div v-if="loading" class="state" :style="{ minHeight: props.minHeight }">
      <div class="spinner" />
      <span class="muted">Loading…</span>
    </div>

    <div v-else-if="error" class="state" :style="{ minHeight: props.minHeight }">
      <div class="state-glyph">⚠️</div>
      <p class="state-title">Couldn’t load this</p>
      <p class="muted state-msg">{{ error }}</p>
      <button class="btn btn-sm" @click="emit('retry')">Retry</button>
    </div>

    <div v-else-if="empty" class="state" :style="{ minHeight: props.minHeight }">
      <div class="state-glyph">🗂️</div>
      <p class="state-title">{{ emptyText }}</p>
      <p v-if="emptyHint" class="muted state-msg">{{ emptyHint }}</p>
    </div>

    <slot v-else />
  </div>
</template>

<style scoped>
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  padding: 28px 16px;
}

.state-glyph {
  font-size: 30px;
}

.state-title {
  margin: 0;
  font-weight: 600;
}

.state-msg {
  margin: 0;
  max-width: 420px;
  font-size: 14px;
}

.spinner {
  width: 26px;
  height: 26px;
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
